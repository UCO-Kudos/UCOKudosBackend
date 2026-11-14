-- V2 · Datos semilla de la línea base (S-03: los usuarios llegan ya activos y verificados)
-- Contraseña de DEMOSTRACIÓN de los tres usuarios: Kudos2026*   (cámbiala fuera del curso)
-- El destinatario usa correo y celular reales tomados de variables de entorno
-- (SEED_DEST_CORREO, SEED_DEST_CELULAR) para no publicar datos personales en el repositorio.
-- Todos los correos son institucionales (@uco.net.co, DOMINIO_INSTITUCIONAL); SEED_DEST_CORREO también debe serlo
-- o el destinatario no podrá iniciar sesión (lo necesita para recibir el push).

INSERT INTO facultad (id, nombre) VALUES
  ('00000000-0000-0000-0000-0000000000f1', 'Ingeniería y Arquitectura');

INSERT INTO programa_academico (id, nombre, facultad_id) VALUES
  ('00000000-0000-0000-0000-0000000000b1', 'Ingeniería de Sistemas', '00000000-0000-0000-0000-0000000000f1');

INSERT INTO categoria (nombre) VALUES
  ('Rendimiento Académico'), ('Mejor Investigador'), ('Empatía'), ('Comunicación Asertiva'),
  ('Orientación al Logro'), ('Trabajo en Equipo'), ('Trabajo Bajo Presión'), ('Visión Estratégica'),
  ('Innovación'), ('Liderazgo'), ('Transformación Digital'), ('Mentor Inspirador'),
  ('Espíritu UCO'), ('Servicio a la Comunidad'), ('Compromiso Institucional');

INSERT INTO persona (id, nombres, apellidos, correo, celular, hash_clave, estado_cuenta, correo_verificado, celular_verificado) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'Administrador', 'Del Sistema', 'admin@uco.net.co', '+570000000001',
   '$2a$10$OxR1g59g35HAQOX2ozoMU.oJ3k3sBchpMmeunqFojXb5MW.GmaBh6', 'ACTIVA', TRUE, TRUE),
  ('00000000-0000-0000-0000-0000000000a2', 'Estudiante', 'Autor', 'autor@uco.net.co', '+570000000002',
   '$2a$10$OxR1g59g35HAQOX2ozoMU.oJ3k3sBchpMmeunqFojXb5MW.GmaBh6', 'ACTIVA', TRUE, TRUE),
  ('00000000-0000-0000-0000-0000000000a3', 'Docente', 'Destinatario', '${seed_dest_correo}', '${seed_dest_celular}',
   '$2a$10$OxR1g59g35HAQOX2ozoMU.oJ3k3sBchpMmeunqFojXb5MW.GmaBh6', 'ACTIVA', TRUE, TRUE);

INSERT INTO asignacion_rol (persona_id, rol, programa_id) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'ADMIN', NULL),
  ('00000000-0000-0000-0000-0000000000a2', 'ESTUDIANTE', '00000000-0000-0000-0000-0000000000b1'),
  ('00000000-0000-0000-0000-0000000000a3', 'DOCENTE', '00000000-0000-0000-0000-0000000000b1');

INSERT INTO persona_programa (persona_id, programa_id, vinculo) VALUES
  ('00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000b1', 'ESTUDIANTE'),
  ('00000000-0000-0000-0000-0000000000a3', '00000000-0000-0000-0000-0000000000b1', 'DOCENTE');
