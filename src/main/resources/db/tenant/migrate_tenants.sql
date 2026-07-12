-- =====================================================================
-- TENANT DATA MIGRATION — run once against MySQL root
-- Copies data from thinkerscave_dev into each tenant schema.
-- All tenant tables were already created by DevDataInitializer (CREATE TABLE ... LIKE).
-- INSERT IGNORE makes this idempotent.
-- =====================================================================

-- =====================================================================
-- TENANT 1: tenant_jsb_bhubaneswar  (Javier School Bhubaneswar, org_id=1)
-- =====================================================================

-- Reference / lookup tables
INSERT IGNORE INTO tenant_jsb_bhubaneswar.roles         SELECT * FROM thinkerscave_dev.roles;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.menus         SELECT * FROM thinkerscave_dev.menus;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.features      SELECT * FROM thinkerscave_dev.features;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.privileges    SELECT * FROM thinkerscave_dev.privileges;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.subscription_plans          SELECT * FROM thinkerscave_dev.subscription_plans;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.subscription_plan_features  SELECT * FROM thinkerscave_dev.subscription_plan_features;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.promotions    SELECT * FROM thinkerscave_dev.promotions;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.responsibility SELECT * FROM thinkerscave_dev.responsibility;

-- Org data
INSERT IGNORE INTO tenant_jsb_bhubaneswar.customers                  SELECT * FROM thinkerscave_dev.customers                  WHERE id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.organizations              SELECT * FROM thinkerscave_dev.organizations              WHERE id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.tenant_registry            SELECT * FROM thinkerscave_dev.tenant_registry            WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.organization_configurations SELECT * FROM thinkerscave_dev.organization_configurations WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.organization_modules       SELECT * FROM thinkerscave_dev.organization_modules       WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.organization_subscriptions SELECT * FROM thinkerscave_dev.organization_subscriptions WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.organization_promotions    SELECT * FROM thinkerscave_dev.organization_promotions    WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.organization_domains       SELECT * FROM thinkerscave_dev.organization_domains       WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.security_policies          SELECT * FROM thinkerscave_dev.security_policies          WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.role_permissions           SELECT * FROM thinkerscave_dev.role_permissions           WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.code_sequence              SELECT * FROM thinkerscave_dev.code_sequence;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.subscription_feature_overrides
    SELECT sfo.* FROM thinkerscave_dev.subscription_feature_overrides sfo
    JOIN thinkerscave_dev.organization_subscriptions os ON sfo.organization_subscription_id = os.id
    WHERE os.organization_id = 1;

-- Users (exclude superadmin who belongs to platform only)
INSERT IGNORE INTO tenant_jsb_bhubaneswar.users
    SELECT * FROM thinkerscave_dev.users WHERE organization_id = 1 AND username <> 'superadmin';
INSERT IGNORE INTO tenant_jsb_bhubaneswar.user_roles
    SELECT ur.* FROM thinkerscave_dev.user_roles ur
    JOIN thinkerscave_dev.users u ON ur.user_id = u.id
    WHERE u.organization_id = 1 AND u.username <> 'superadmin';
INSERT IGNORE INTO tenant_jsb_bhubaneswar.user_permissions
    SELECT up.* FROM thinkerscave_dev.user_permissions up
    JOIN thinkerscave_dev.users u ON up.user_id = u.id
    WHERE u.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.login_history
    SELECT lh.* FROM thinkerscave_dev.login_history lh
    JOIN thinkerscave_dev.users u ON lh.user_id = u.id
    WHERE u.organization_id = 1;

-- Academic reference data (no org scope — copy all)
INSERT IGNORE INTO tenant_jsb_bhubaneswar.academic_year     SELECT * FROM thinkerscave_dev.academic_year;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.academic_class    SELECT * FROM thinkerscave_dev.academic_class;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.academic_section  SELECT * FROM thinkerscave_dev.academic_section;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.academic_setting  SELECT * FROM thinkerscave_dev.academic_setting;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.attendance_setting SELECT * FROM thinkerscave_dev.attendance_setting;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.subject           SELECT * FROM thinkerscave_dev.subject;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.period_template   SELECT * FROM thinkerscave_dev.period_template;

-- Staff for org 1
INSERT IGNORE INTO tenant_jsb_bhubaneswar.staff
    SELECT s.* FROM thinkerscave_dev.staff s
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.staff_salary_structure
    SELECT ss.* FROM thinkerscave_dev.staff_salary_structure ss
    JOIN thinkerscave_dev.staff s ON ss.staff_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.payroll
    SELECT p.* FROM thinkerscave_dev.payroll p
    JOIN thinkerscave_dev.staff s ON p.staff_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.responsibility_assignment
    SELECT ra.* FROM thinkerscave_dev.responsibility_assignment ra
    JOIN thinkerscave_dev.staff s ON ra.staff_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.staff_attendance
    SELECT * FROM thinkerscave_dev.staff_attendance WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.subject_assignment
    SELECT sa.* FROM thinkerscave_dev.subject_assignment sa
    JOIN thinkerscave_dev.staff s ON sa.teacher_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 1;

-- Students for org 1
INSERT IGNORE INTO tenant_jsb_bhubaneswar.parent
    SELECT p.* FROM thinkerscave_dev.parent p
    LEFT JOIN thinkerscave_dev.users u ON p.user_id = u.id
    WHERE u.organization_id = 1 OR (p.user_id IS NULL AND p.parent_code LIKE 'PAR%'
      AND p.parent_id IN (SELECT sp.parent_id FROM thinkerscave_dev.student_parent sp
                          JOIN thinkerscave_dev.student st ON sp.student_id = st.student_id
                          WHERE st.student_code LIKE 'JSB-%'));
INSERT IGNORE INTO tenant_jsb_bhubaneswar.student
    SELECT * FROM thinkerscave_dev.student WHERE student_code LIKE 'JSB-%';
INSERT IGNORE INTO tenant_jsb_bhubaneswar.student_enrollment
    SELECT se.* FROM thinkerscave_dev.student_enrollment se
    JOIN thinkerscave_dev.student st ON se.student_id = st.student_id
    WHERE st.student_code LIKE 'JSB-%';
INSERT IGNORE INTO tenant_jsb_bhubaneswar.student_parent
    SELECT sp.* FROM thinkerscave_dev.student_parent sp
    JOIN thinkerscave_dev.student st ON sp.student_id = st.student_id
    WHERE st.student_code LIKE 'JSB-%';
INSERT IGNORE INTO tenant_jsb_bhubaneswar.student_medical
    SELECT sm.* FROM thinkerscave_dev.student_medical sm
    JOIN thinkerscave_dev.student st ON sm.student_id = st.student_id
    WHERE st.student_code LIKE 'JSB-%';
INSERT IGNORE INTO tenant_jsb_bhubaneswar.student_attendance
    SELECT * FROM thinkerscave_dev.student_attendance WHERE organization_id = 1;

-- Communication for org 1
INSERT IGNORE INTO tenant_jsb_bhubaneswar.notice
    SELECT * FROM thinkerscave_dev.notice WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.notice_audience
    SELECT na.* FROM thinkerscave_dev.notice_audience na
    JOIN thinkerscave_dev.notice n ON na.notice_id = n.notice_id
    WHERE n.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.notification
    SELECT * FROM thinkerscave_dev.notification WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.notification_recipient
    SELECT nr.* FROM thinkerscave_dev.notification_recipient nr
    JOIN thinkerscave_dev.notification nf ON nr.notification_id = nf.notification_id
    WHERE nf.organization_id = 1;

-- Admission for org 1
INSERT IGNORE INTO tenant_jsb_bhubaneswar.inquiry
    SELECT * FROM thinkerscave_dev.inquiry WHERE organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.inquiry_follow_up
    SELECT iff.* FROM thinkerscave_dev.inquiry_follow_up iff
    JOIN thinkerscave_dev.inquiry i ON iff.inquiry_id = i.inquiry_id
    WHERE i.organization_id = 1;
INSERT IGNORE INTO tenant_jsb_bhubaneswar.application_admission
    SELECT * FROM thinkerscave_dev.application_admission WHERE organization_id = 1;


-- =====================================================================
-- TENANT 2: tenant_jsc_cuttack  (Javier School Cuttack, org_id=2)
-- =====================================================================

INSERT IGNORE INTO tenant_jsc_cuttack.roles         SELECT * FROM thinkerscave_dev.roles;
INSERT IGNORE INTO tenant_jsc_cuttack.menus         SELECT * FROM thinkerscave_dev.menus;
INSERT IGNORE INTO tenant_jsc_cuttack.features      SELECT * FROM thinkerscave_dev.features;
INSERT IGNORE INTO tenant_jsc_cuttack.privileges    SELECT * FROM thinkerscave_dev.privileges;
INSERT IGNORE INTO tenant_jsc_cuttack.subscription_plans         SELECT * FROM thinkerscave_dev.subscription_plans;
INSERT IGNORE INTO tenant_jsc_cuttack.subscription_plan_features SELECT * FROM thinkerscave_dev.subscription_plan_features;
INSERT IGNORE INTO tenant_jsc_cuttack.promotions    SELECT * FROM thinkerscave_dev.promotions;
INSERT IGNORE INTO tenant_jsc_cuttack.responsibility SELECT * FROM thinkerscave_dev.responsibility;

INSERT IGNORE INTO tenant_jsc_cuttack.customers                   SELECT * FROM thinkerscave_dev.customers                   WHERE id = 1;
INSERT IGNORE INTO tenant_jsc_cuttack.organizations               SELECT * FROM thinkerscave_dev.organizations               WHERE id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.tenant_registry             SELECT * FROM thinkerscave_dev.tenant_registry             WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.organization_configurations  SELECT * FROM thinkerscave_dev.organization_configurations  WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.organization_modules        SELECT * FROM thinkerscave_dev.organization_modules        WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.organization_subscriptions  SELECT * FROM thinkerscave_dev.organization_subscriptions  WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.organization_promotions     SELECT * FROM thinkerscave_dev.organization_promotions     WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.organization_domains        SELECT * FROM thinkerscave_dev.organization_domains        WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.security_policies           SELECT * FROM thinkerscave_dev.security_policies           WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.role_permissions            SELECT * FROM thinkerscave_dev.role_permissions            WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.code_sequence               SELECT * FROM thinkerscave_dev.code_sequence;
INSERT IGNORE INTO tenant_jsc_cuttack.subscription_feature_overrides
    SELECT sfo.* FROM thinkerscave_dev.subscription_feature_overrides sfo
    JOIN thinkerscave_dev.organization_subscriptions os ON sfo.organization_subscription_id = os.id
    WHERE os.organization_id = 2;

INSERT IGNORE INTO tenant_jsc_cuttack.users
    SELECT * FROM thinkerscave_dev.users WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.user_roles
    SELECT ur.* FROM thinkerscave_dev.user_roles ur
    JOIN thinkerscave_dev.users u ON ur.user_id = u.id
    WHERE u.organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.user_permissions
    SELECT up.* FROM thinkerscave_dev.user_permissions up
    JOIN thinkerscave_dev.users u ON up.user_id = u.id
    WHERE u.organization_id = 2;

INSERT IGNORE INTO tenant_jsc_cuttack.academic_year     SELECT * FROM thinkerscave_dev.academic_year;
INSERT IGNORE INTO tenant_jsc_cuttack.academic_class    SELECT * FROM thinkerscave_dev.academic_class;
INSERT IGNORE INTO tenant_jsc_cuttack.academic_section  SELECT * FROM thinkerscave_dev.academic_section;
INSERT IGNORE INTO tenant_jsc_cuttack.academic_setting  SELECT * FROM thinkerscave_dev.academic_setting;
INSERT IGNORE INTO tenant_jsc_cuttack.attendance_setting SELECT * FROM thinkerscave_dev.attendance_setting;
INSERT IGNORE INTO tenant_jsc_cuttack.subject           SELECT * FROM thinkerscave_dev.subject;

INSERT IGNORE INTO tenant_jsc_cuttack.staff
    SELECT s.* FROM thinkerscave_dev.staff s
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.staff_attendance
    SELECT * FROM thinkerscave_dev.staff_attendance WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.student
    SELECT * FROM thinkerscave_dev.student WHERE student_code LIKE 'JSC-%';
INSERT IGNORE INTO tenant_jsc_cuttack.student_enrollment
    SELECT se.* FROM thinkerscave_dev.student_enrollment se
    JOIN thinkerscave_dev.student st ON se.student_id = st.student_id
    WHERE st.student_code LIKE 'JSC-%';
INSERT IGNORE INTO tenant_jsc_cuttack.student_attendance
    SELECT * FROM thinkerscave_dev.student_attendance WHERE organization_id = 2;

INSERT IGNORE INTO tenant_jsc_cuttack.notice
    SELECT * FROM thinkerscave_dev.notice WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.notice_audience
    SELECT na.* FROM thinkerscave_dev.notice_audience na
    JOIN thinkerscave_dev.notice n ON na.notice_id = n.notice_id
    WHERE n.organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.inquiry
    SELECT * FROM thinkerscave_dev.inquiry WHERE organization_id = 2;
INSERT IGNORE INTO tenant_jsc_cuttack.inquiry_follow_up
    SELECT iff.* FROM thinkerscave_dev.inquiry_follow_up iff
    JOIN thinkerscave_dev.inquiry i ON iff.inquiry_id = i.inquiry_id
    WHERE i.organization_id = 2;


-- =====================================================================
-- TENANT 3: tenant_abc_puri  (ABC School Puri, org_id=3)
-- =====================================================================

INSERT IGNORE INTO tenant_abc_puri.roles         SELECT * FROM thinkerscave_dev.roles;
INSERT IGNORE INTO tenant_abc_puri.menus         SELECT * FROM thinkerscave_dev.menus;
INSERT IGNORE INTO tenant_abc_puri.features      SELECT * FROM thinkerscave_dev.features;
INSERT IGNORE INTO tenant_abc_puri.privileges    SELECT * FROM thinkerscave_dev.privileges;
INSERT IGNORE INTO tenant_abc_puri.subscription_plans         SELECT * FROM thinkerscave_dev.subscription_plans;
INSERT IGNORE INTO tenant_abc_puri.subscription_plan_features SELECT * FROM thinkerscave_dev.subscription_plan_features;
INSERT IGNORE INTO tenant_abc_puri.promotions    SELECT * FROM thinkerscave_dev.promotions;
INSERT IGNORE INTO tenant_abc_puri.responsibility SELECT * FROM thinkerscave_dev.responsibility;

INSERT IGNORE INTO tenant_abc_puri.customers                   SELECT * FROM thinkerscave_dev.customers                   WHERE id = 2;
INSERT IGNORE INTO tenant_abc_puri.organizations               SELECT * FROM thinkerscave_dev.organizations               WHERE id = 3;
INSERT IGNORE INTO tenant_abc_puri.tenant_registry             SELECT * FROM thinkerscave_dev.tenant_registry             WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.organization_configurations  SELECT * FROM thinkerscave_dev.organization_configurations  WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.organization_modules        SELECT * FROM thinkerscave_dev.organization_modules        WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.organization_subscriptions  SELECT * FROM thinkerscave_dev.organization_subscriptions  WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.organization_promotions     SELECT * FROM thinkerscave_dev.organization_promotions     WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.organization_domains        SELECT * FROM thinkerscave_dev.organization_domains        WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.security_policies           SELECT * FROM thinkerscave_dev.security_policies           WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.role_permissions            SELECT * FROM thinkerscave_dev.role_permissions            WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.code_sequence               SELECT * FROM thinkerscave_dev.code_sequence;
INSERT IGNORE INTO tenant_abc_puri.subscription_feature_overrides
    SELECT sfo.* FROM thinkerscave_dev.subscription_feature_overrides sfo
    JOIN thinkerscave_dev.organization_subscriptions os ON sfo.organization_subscription_id = os.id
    WHERE os.organization_id = 3;

INSERT IGNORE INTO tenant_abc_puri.users
    SELECT * FROM thinkerscave_dev.users WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.user_roles
    SELECT ur.* FROM thinkerscave_dev.user_roles ur
    JOIN thinkerscave_dev.users u ON ur.user_id = u.id
    WHERE u.organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.user_permissions
    SELECT up.* FROM thinkerscave_dev.user_permissions up
    JOIN thinkerscave_dev.users u ON up.user_id = u.id
    WHERE u.organization_id = 3;

INSERT IGNORE INTO tenant_abc_puri.academic_year     SELECT * FROM thinkerscave_dev.academic_year;
INSERT IGNORE INTO tenant_abc_puri.academic_class    SELECT * FROM thinkerscave_dev.academic_class;
INSERT IGNORE INTO tenant_abc_puri.academic_section  SELECT * FROM thinkerscave_dev.academic_section;
INSERT IGNORE INTO tenant_abc_puri.academic_setting  SELECT * FROM thinkerscave_dev.academic_setting;
INSERT IGNORE INTO tenant_abc_puri.attendance_setting SELECT * FROM thinkerscave_dev.attendance_setting;
INSERT IGNORE INTO tenant_abc_puri.subject           SELECT * FROM thinkerscave_dev.subject;

INSERT IGNORE INTO tenant_abc_puri.staff
    SELECT s.* FROM thinkerscave_dev.staff s
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.staff_salary_structure
    SELECT ss.* FROM thinkerscave_dev.staff_salary_structure ss
    JOIN thinkerscave_dev.staff s ON ss.staff_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.responsibility_assignment
    SELECT ra.* FROM thinkerscave_dev.responsibility_assignment ra
    JOIN thinkerscave_dev.staff s ON ra.staff_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.staff_attendance
    SELECT * FROM thinkerscave_dev.staff_attendance WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.parent
    SELECT p.* FROM thinkerscave_dev.parent p
    LEFT JOIN thinkerscave_dev.users u ON p.user_id = u.id
    WHERE u.organization_id = 3 OR (p.user_id IS NULL AND p.parent_id IN
          (SELECT sp.parent_id FROM thinkerscave_dev.student_parent sp
           JOIN thinkerscave_dev.student st ON sp.student_id = st.student_id
           WHERE st.student_code LIKE 'ABCP-%'));
INSERT IGNORE INTO tenant_abc_puri.student
    SELECT * FROM thinkerscave_dev.student WHERE student_code LIKE 'ABCP-%';
INSERT IGNORE INTO tenant_abc_puri.student_enrollment
    SELECT se.* FROM thinkerscave_dev.student_enrollment se
    JOIN thinkerscave_dev.student st ON se.student_id = st.student_id
    WHERE st.student_code LIKE 'ABCP-%';
INSERT IGNORE INTO tenant_abc_puri.student_parent
    SELECT sp.* FROM thinkerscave_dev.student_parent sp
    JOIN thinkerscave_dev.student st ON sp.student_id = st.student_id
    WHERE st.student_code LIKE 'ABCP-%';
INSERT IGNORE INTO tenant_abc_puri.student_medical
    SELECT sm.* FROM thinkerscave_dev.student_medical sm
    JOIN thinkerscave_dev.student st ON sm.student_id = st.student_id
    WHERE st.student_code LIKE 'ABCP-%';
INSERT IGNORE INTO tenant_abc_puri.student_attendance
    SELECT * FROM thinkerscave_dev.student_attendance WHERE organization_id = 3;

INSERT IGNORE INTO tenant_abc_puri.notice
    SELECT * FROM thinkerscave_dev.notice WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.notice_audience
    SELECT na.* FROM thinkerscave_dev.notice_audience na
    JOIN thinkerscave_dev.notice n ON na.notice_id = n.notice_id
    WHERE n.organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.inquiry
    SELECT * FROM thinkerscave_dev.inquiry WHERE organization_id = 3;
INSERT IGNORE INTO tenant_abc_puri.inquiry_follow_up
    SELECT iff.* FROM thinkerscave_dev.inquiry_follow_up iff
    JOIN thinkerscave_dev.inquiry i ON iff.inquiry_id = i.inquiry_id
    WHERE i.organization_id = 3;


-- =====================================================================
-- TENANT 4: tenant_kcc_cuttack  (Kalinga College Cuttack, org_id=4)
-- =====================================================================

INSERT IGNORE INTO tenant_kcc_cuttack.roles         SELECT * FROM thinkerscave_dev.roles;
INSERT IGNORE INTO tenant_kcc_cuttack.menus         SELECT * FROM thinkerscave_dev.menus;
INSERT IGNORE INTO tenant_kcc_cuttack.features      SELECT * FROM thinkerscave_dev.features;
INSERT IGNORE INTO tenant_kcc_cuttack.privileges    SELECT * FROM thinkerscave_dev.privileges;
INSERT IGNORE INTO tenant_kcc_cuttack.subscription_plans         SELECT * FROM thinkerscave_dev.subscription_plans;
INSERT IGNORE INTO tenant_kcc_cuttack.subscription_plan_features SELECT * FROM thinkerscave_dev.subscription_plan_features;
INSERT IGNORE INTO tenant_kcc_cuttack.promotions    SELECT * FROM thinkerscave_dev.promotions;
INSERT IGNORE INTO tenant_kcc_cuttack.responsibility SELECT * FROM thinkerscave_dev.responsibility;

INSERT IGNORE INTO tenant_kcc_cuttack.customers                   SELECT * FROM thinkerscave_dev.customers                   WHERE id = 3;
INSERT IGNORE INTO tenant_kcc_cuttack.organizations               SELECT * FROM thinkerscave_dev.organizations               WHERE id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.tenant_registry             SELECT * FROM thinkerscave_dev.tenant_registry             WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.organization_configurations  SELECT * FROM thinkerscave_dev.organization_configurations  WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.organization_modules        SELECT * FROM thinkerscave_dev.organization_modules        WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.organization_subscriptions  SELECT * FROM thinkerscave_dev.organization_subscriptions  WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.organization_promotions     SELECT * FROM thinkerscave_dev.organization_promotions     WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.organization_domains        SELECT * FROM thinkerscave_dev.organization_domains        WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.security_policies           SELECT * FROM thinkerscave_dev.security_policies           WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.role_permissions            SELECT * FROM thinkerscave_dev.role_permissions            WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.code_sequence               SELECT * FROM thinkerscave_dev.code_sequence;
INSERT IGNORE INTO tenant_kcc_cuttack.subscription_feature_overrides
    SELECT sfo.* FROM thinkerscave_dev.subscription_feature_overrides sfo
    JOIN thinkerscave_dev.organization_subscriptions os ON sfo.organization_subscription_id = os.id
    WHERE os.organization_id = 4;

INSERT IGNORE INTO tenant_kcc_cuttack.users
    SELECT * FROM thinkerscave_dev.users WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.user_roles
    SELECT ur.* FROM thinkerscave_dev.user_roles ur
    JOIN thinkerscave_dev.users u ON ur.user_id = u.id
    WHERE u.organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.user_permissions
    SELECT up.* FROM thinkerscave_dev.user_permissions up
    JOIN thinkerscave_dev.users u ON up.user_id = u.id
    WHERE u.organization_id = 4;

INSERT IGNORE INTO tenant_kcc_cuttack.academic_year     SELECT * FROM thinkerscave_dev.academic_year;
INSERT IGNORE INTO tenant_kcc_cuttack.academic_class    SELECT * FROM thinkerscave_dev.academic_class;
INSERT IGNORE INTO tenant_kcc_cuttack.academic_section  SELECT * FROM thinkerscave_dev.academic_section;
INSERT IGNORE INTO tenant_kcc_cuttack.academic_setting  SELECT * FROM thinkerscave_dev.academic_setting;
INSERT IGNORE INTO tenant_kcc_cuttack.attendance_setting SELECT * FROM thinkerscave_dev.attendance_setting;
INSERT IGNORE INTO tenant_kcc_cuttack.subject           SELECT * FROM thinkerscave_dev.subject;

INSERT IGNORE INTO tenant_kcc_cuttack.staff
    SELECT s.* FROM thinkerscave_dev.staff s
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.staff_salary_structure
    SELECT ss.* FROM thinkerscave_dev.staff_salary_structure ss
    JOIN thinkerscave_dev.staff s ON ss.staff_id = s.staff_id
    JOIN thinkerscave_dev.users u ON s.user_id = u.id
    WHERE u.organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.staff_attendance
    SELECT * FROM thinkerscave_dev.staff_attendance WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.parent
    SELECT p.* FROM thinkerscave_dev.parent p
    LEFT JOIN thinkerscave_dev.users u ON p.user_id = u.id
    WHERE u.organization_id = 4 OR (p.user_id IS NULL AND p.parent_id IN
          (SELECT sp.parent_id FROM thinkerscave_dev.student_parent sp
           JOIN thinkerscave_dev.student st ON sp.student_id = st.student_id
           WHERE st.student_code LIKE 'KCC-%'));
INSERT IGNORE INTO tenant_kcc_cuttack.student
    SELECT * FROM thinkerscave_dev.student WHERE student_code LIKE 'KCC-%';
INSERT IGNORE INTO tenant_kcc_cuttack.student_enrollment
    SELECT se.* FROM thinkerscave_dev.student_enrollment se
    JOIN thinkerscave_dev.student st ON se.student_id = st.student_id
    WHERE st.student_code LIKE 'KCC-%';
INSERT IGNORE INTO tenant_kcc_cuttack.student_parent
    SELECT sp.* FROM thinkerscave_dev.student_parent sp
    JOIN thinkerscave_dev.student st ON sp.student_id = st.student_id
    WHERE st.student_code LIKE 'KCC-%';
INSERT IGNORE INTO tenant_kcc_cuttack.student_medical
    SELECT sm.* FROM thinkerscave_dev.student_medical sm
    JOIN thinkerscave_dev.student st ON sm.student_id = st.student_id
    WHERE st.student_code LIKE 'KCC-%';
INSERT IGNORE INTO tenant_kcc_cuttack.student_attendance
    SELECT * FROM thinkerscave_dev.student_attendance WHERE organization_id = 4;

INSERT IGNORE INTO tenant_kcc_cuttack.notice
    SELECT * FROM thinkerscave_dev.notice WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.notice_audience
    SELECT na.* FROM thinkerscave_dev.notice_audience na
    JOIN thinkerscave_dev.notice n ON na.notice_id = n.notice_id
    WHERE n.organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.notification
    SELECT * FROM thinkerscave_dev.notification WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.inquiry
    SELECT * FROM thinkerscave_dev.inquiry WHERE organization_id = 4;
INSERT IGNORE INTO tenant_kcc_cuttack.inquiry_follow_up
    SELECT iff.* FROM thinkerscave_dev.inquiry_follow_up iff
    JOIN thinkerscave_dev.inquiry i ON iff.inquiry_id = i.inquiry_id
    WHERE i.organization_id = 4;
