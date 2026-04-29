-- ============================================================================
-- V1_7: Seed default Classes and Sections for student registration
-- ============================================================================

-- Insert default classes (grades 1-12)
INSERT INTO public.class (class_name) VALUES
  ('Grade 1'),
  ('Grade 2'),
  ('Grade 3'),
  ('Grade 4'),
  ('Grade 5'),
  ('Grade 6'),
  ('Grade 7'),
  ('Grade 8'),
  ('Grade 9'),
  ('Grade 10'),
  ('Grade 11'),
  ('Grade 12')
ON CONFLICT DO NOTHING;

-- Insert default sections (A, B, C, D) for each class
-- Note: FK column is class_entity_class_id (from @JoinColumn in Section.java)
INSERT INTO public.section (section_name, class_entity_class_id)
SELECT s.section_name, c.class_id
FROM public.class c
CROSS JOIN (
  VALUES ('Section A'), ('Section B'), ('Section C'), ('Section D')
) AS s(section_name)
ON CONFLICT DO NOTHING;
