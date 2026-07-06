-- btree_gist da las operator classes gist para tipos escalares (int, varchar...),
-- necesario para combinar 'space_id WITH =' junto a un rango 'WITH &&' en la
-- exclusion constraint de reservas (ver V5).
create extension if not exists btree_gist;
