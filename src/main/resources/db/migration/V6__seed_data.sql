-- Admin inicial para poder operar la API recien levantada (el registro abierto
-- solo crea USER). Password: admin1234 (BCrypt). Cambiar en cualquier entorno real.
insert into users (email, password_hash, full_name, role, created_at)
values ('admin@coworking.sv',
        '$2b$10$gfRTo5aI.3c4sPlJGOxVgOOSVihl9IGf0Kx4WwMq/hKMArpgLkmve',
        'Administrador', 'ADMIN', now())
on conflict (email) do nothing;

-- Espacios de demo para que la coleccion .http funcione de una. version=0 para que
-- el optimistic lock de JPA arranque bien al editarlos.
insert into spaces (name, type, capacity, location, hourly_rate, active, version) values
    ('Sala Izalco',        'MEETING_ROOM', 8, 'Piso 3', 12.50, true, 0),
    ('Puesto Flex 12',     'DESK',         1, 'Piso 2',  3.00, true, 0),
    ('Cabina Cerro Verde', 'PHONE_BOOTH',  2, 'Piso 1',  5.00, true, 0);
