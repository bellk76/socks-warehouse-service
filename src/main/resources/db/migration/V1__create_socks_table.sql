CREATE TABLE socks (
    id          BIGSERIAL PRIMARY KEY,
    color       VARCHAR(50) NOT NULL,
    cotton_part INTEGER     NOT NULL,
    quantity    BIGINT      NOT NULL,
    CONSTRAINT ck_socks_cotton_part CHECK (cotton_part BETWEEN 0 AND 100),
    CONSTRAINT ck_socks_quantity CHECK (quantity >= 0),
    CONSTRAINT uq_socks_color_cotton UNIQUE (color, cotton_part)
);

COMMENT ON TABLE socks IS 'Остатки носков на складе: одна строка на пару цвет + % хлопка';
