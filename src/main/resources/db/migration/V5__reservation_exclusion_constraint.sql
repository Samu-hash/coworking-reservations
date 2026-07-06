-- Garantia dura contra reservas solapadas del mismo espacio, a nivel motor.
-- No depende del codigo de la app ni del nivel de aislamiento: dos inserts que se
-- pisan (mismo space_id, rangos que se tocan) no pueden coexistir.
--
-- Detalles que importan:
--  * el rango va como expresion inline tstzrange(...) '[)' (half-open): una reserva
--    09:00-10:00 y otra 10:00-11:00 NO se solapan.
--  * WHERE parcial: solo PENDING_PAYMENT y CONFIRMED ocupan el slot. Al cancelar,
--    la reserva sale del indice y libera el horario sin borrar la fila.
alter table reservations
    add constraint reservations_no_overlap
    exclude using gist (
        space_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) where (status in ('PENDING_PAYMENT', 'CONFIRMED'));
