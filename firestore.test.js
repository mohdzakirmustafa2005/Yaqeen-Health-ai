const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function validUserProfile(uid) {
  const ts = pastDate(5000);
  return {
    userId: uid,
    displayName: "Amina Rahman",
    department: "Operations",
    organizationId: "crescent-health",
    role: "INDIVIDUAL",
    language: "EN",
    consentAnalytics: true,
    consentAiGuidance: true,
    consentCoordinatorOutreach: false,
    remindersEnabled: true,
    reminderHour: 9,
    reminderMinute: 0,
    onboardingCompleted: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCheckIn(uid) {
  const ts = pastDate(3000);
  return {
    userId: uid,
    dateKey: "2026-11-19",
    overallWellbeing: 7,
    sleepQuality: 6,
    sleepHours: 6.5,
    stressLevel: 6,
    energyLevel: 7,
    fatigueLevel: 5,
    mood: "CALM",
    symptoms: ["Mild neck tension"],
    workStress: 6,
    hydrationLiters: 1.8,
    activityMinutes: 30,
    notes: "Steady afternoon focus after short break.",
    computedRiskLevel: "YELLOW",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or check-ins", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("checkIns").get());
});

test("Owner can create and partially update UserProfile; cannot self-escalate role or inject ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validUserProfile(ALICE_UID)));

  // Valid partial update
  await assertSucceeds(
    profileRef.update({
      language: "UR",
      consentCoordinatorOutreach: true,
      updatedAt: pastDate(500),
    })
  );

  // Role escalation rejected
  await assertFails(
    profileRef.update({
      role: "PLATFORM_ADMIN",
      updatedAt: pastDate(500),
    })
  );

  // Self-assigned admin on create rejected
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const badProfile = { ...validUserProfile(BOB_UID), role: "ORG_ADMIN" };
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(badProfile));

  // Shadow update with ghost field rejected
  await assertFails(
    profileRef.update({
      isVerified: true,
      updatedAt: pastDate(500),
    })
  );
});

test("Admin email spoofing with email_verified=false is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext(BOB_UID, {
    email: "mohdzakirmustafa1@gmail.com",
    email_verified: false,
  }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID));
  });

  await assertFails(spoofDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read or write Alice's health check-ins or alerts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("checkIns").doc("ci_1").set(validCheckIn(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("checkIns").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("checkIns").doc("ci_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("checkIns").doc("ci_2").set(validCheckIn(ALICE_UID))
  );
});

test("Check-in rejects @DocumentId 'id' field, future timestamps, and out-of-bounds values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("checkIns");

  // Extra 'id' field rejected
  await assertFails(col.doc("ci_bad_id").set({ ...validCheckIn(ALICE_UID), id: "ci_bad_id" }));

  // Future timestamp rejected
  await assertFails(
    col.doc("ci_future").set({ ...validCheckIn(ALICE_UID), createdAt: new Date(Date.now() + 120000) })
  );

  // Invalid wellbeing > 10 rejected
  await assertFails(col.doc("ci_oob").set({ ...validCheckIn(ALICE_UID), overallWellbeing: 15 }));

  // Empty collection list succeeds for owner
  await assertSucceeds(col.get());
});

test("Alerts enforce terminal state locking and recommendations allow completion toggle", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const alertRef = aliceDb.collection("users").doc(ALICE_UID).collection("alerts").doc("alert_1");

  await assertSucceeds(
    alertRef.set({
      userId: ALICE_UID,
      riskLevel: "ORANGE",
      title: "Elevated Fatigue & Stress Pattern",
      explanation: "Stress >= 7 and sleep < 6h over consecutive check-ins.",
      recommendedNextStep: "Schedule rest recovery and speak with your health coordinator.",
      triggerFactors: ["High stress", "Short sleep"],
      dateKey: "2026-11-19",
      status: "ACTIVE",
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );

  // Transition ACTIVE -> ACKNOWLEDGED succeeds
  await assertSucceeds(alertRef.update({ status: "ACKNOWLEDGED", updatedAt: pastDate(1000) }));

  // Subsequent transition ACKNOWLEDGED -> ACTIVE fails (terminal lock)
  await assertFails(alertRef.update({ status: "ACTIVE", updatedAt: pastDate(500) }));

  // Recommendations create + toggle completed
  const recRef = aliceDb.collection("users").doc(ALICE_UID).collection("recommendations").doc("rec_1");
  await assertSucceeds(
    recRef.set({
      userId: ALICE_UID,
      category: "SLEEP",
      title: "Consistent Wind-Down Window",
      description: "Dim screens 45 minutes before target sleep time.",
      frequency: "DAILY",
      completed: false,
      source: "ENGINE",
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(recRef.update({ completed: true, updatedAt: pastDate(500) }));
});

test("Audit logs are strictly append-only (update and delete rejected)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1");

  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      action: "CONSENT_UPDATED",
      details: "Enabled AI wellness guidance.",
      actorRole: "INDIVIDUAL",
      createdAt: pastDate(2000),
    })
  );

  await assertFails(logRef.update({ action: "TAMPERED" }));
  await assertFails(logRef.delete());
});

test("Doctor conversations, structured messages, and feedback updates respect ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const convRef = aliceDb.collection("users").doc(ALICE_UID).collection("doctorConversations").doc("conv_1");
  await assertSucceeds(
    convRef.set({
      userId: ALICE_UID,
      domain: "HEART_SPECIALIST",
      mode: "HEART_SPECIALIST",
      title: "Blood pressure & palpitation questions",
      lastPreview: "Asked about resting HR 78 bpm",
      messageCount: 2,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    convRef.update({ lastPreview: "Updated preview", messageCount: 3, updatedAt: pastDate(1000) })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("doctorConversations").doc("conv_1").get()
  );

  const msgRef = aliceDb.collection("users").doc(ALICE_UID).collection("doctorMessages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      conversationId: "conv_1",
      sender: "ASSISTANT",
      mode: "HEART_SPECIALIST",
      taskType: "HEART_MEASUREMENT_EXPLANATION",
      text: "Your entered resting heart rate is 78 bpm.",
      providerUsed: "Firebase AI Logic",
      feedback: 0,
      emergencyTriggered: false,
      fallbackUsed: false,
      language: "EN",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );
  await assertSucceeds(msgRef.update({ feedback: 1, updatedAt: pastDate(500) }));
  await assertFails(msgRef.update({ text: "Tampered", updatedAt: pastDate(200) }));
});

test("Heart measurements, BP records, symptom reports, reports, safety events, and consents enforce bounds & immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const measCol = aliceDb.collection("users").doc(ALICE_UID).collection("heartMeasurements");
  await assertSucceeds(
    measCol.doc("m_1").set({
      userId: ALICE_UID,
      recordedAtIso: "2026-11-19 08:30",
      restingHeartRateBpm: 72,
      systolicMmHg: 118,
      diastolicMmHg: 76,
      oxygenSaturationPct: 98,
      weightKg: 70.5,
      symptoms: [],
      activityLevel: "RESTING",
      sourceLabel: "USER_ENTERED",
      emergencyFlagged: false,
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );
  // Out-of-bounds heart rate > 250 rejected
  await assertFails(
    measCol.doc("m_bad").set({
      userId: ALICE_UID,
      recordedAtIso: "2026-11-19 08:30",
      restingHeartRateBpm: 500,
      symptoms: [],
      activityLevel: "RESTING",
      sourceLabel: "USER_ENTERED",
      emergencyFlagged: false,
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Blood pressure record
  const bpRef = aliceDb.collection("users").doc(ALICE_UID).collection("bloodPressureRecords").doc("bp_1");
  await assertSucceeds(
    bpRef.set({
      userId: ALICE_UID,
      measurementId: "m_1",
      recordedAtIso: "2026-11-19 08:30",
      systolicMmHg: 118,
      diastolicMmHg: 76,
      postureOrContext: "SITTING_REST",
      sourceLabel: "USER_ENTERED",
      createdAt: pastDate(2000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bloodPressureRecords").get());

  // Safety event & consent record are immutable
  const safetyRef = aliceDb.collection("users").doc(ALICE_UID).collection("medicalSafetyEvents").doc("se_1");
  await assertSucceeds(
    safetyRef.set({
      userId: ALICE_UID,
      sourceModule: "HEART_SYMPTOM_CHECKER",
      matchedSignals: ["Chest pain"],
      reason: "Chest tightness reported.",
      actionTaken: "BLOCKED_WELLNESS_FLOW_SHOWED_EMERGENCY_GUIDANCE",
      emergencyRegionHint: "INDIA_112_OR_LOCAL_EMS",
      createdAt: pastDate(2000),
    })
  );
  await assertFails(safetyRef.delete());

  const consentRef = aliceDb.collection("users").doc(ALICE_UID).collection("consentRecords").doc("c_1");
  await assertSucceeds(
    consentRef.set({
      userId: ALICE_UID,
      consentAnalytics: true,
      consentAiGuidance: true,
      consentHeartAiContext: true,
      consentReportAnalysis: true,
      consentCoordinatorOutreach: false,
      policyNote: "Enabled heart specialist AI context.",
      createdAt: pastDate(2000),
    })
  );
  await assertFails(consentRef.update({ consentHeartAiContext: false }));
});

