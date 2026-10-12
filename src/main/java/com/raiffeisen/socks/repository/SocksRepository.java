package com.raiffeisen.socks.repository;

import com.raiffeisen.socks.model.Socks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocksRepository extends JpaRepository<Socks, Long> {

    /**
     * Атомарный приход: вставляет новую строку или инкрементирует количество.
     * ON CONFLICT защищает от гонок при одновременном приходе одних и тех же носков.
     */
    @Modifying
    @Query(value = """
            INSERT INTO socks (color, cotton_part, quantity)
            VALUES (:color, :cottonPart, :quantity)
            ON CONFLICT (color, cotton_part)
            DO UPDATE SET quantity = socks.quantity + EXCLUDED.quantity
            """, nativeQuery = true)
    void upsertIncome(@Param("color") String color,
                      @Param("cottonPart") Integer cottonPart,
                      @Param("quantity") Long quantity);

    /**
     * Атомарный расход: уменьшает количество, если хватает остатка.
     * Возвращает новое количество (RETURNING); null — носков не хватает (или строки нет) → бизнес-ошибка 400.
     * Без @Modifying: запрос должен выполниться как читающий, чтобы прочитать результат RETURNING.
     */
    @Query(value = """
            UPDATE socks
            SET quantity = quantity - :quantity
            WHERE color = :color AND cotton_part = :cottonPart AND quantity >= :quantity
            RETURNING quantity
            """, nativeQuery = true)
    Long decreaseQuantity(@Param("color") String color,
                          @Param("cottonPart") Integer cottonPart,
                          @Param("quantity") Long quantity);

    /**
     * Удаляет строку с нулевым количеством (очистка после полного распродажи).
     */
    @Modifying
    @Query(value = """
            DELETE FROM socks
            WHERE color = :color AND cotton_part = :cottonPart AND quantity = 0
            """, nativeQuery = true)
    int deleteIfZero(@Param("color") String color,
                     @Param("cottonPart") Integer cottonPart);

    /**
     * Суммарное количество по цвету и фильтру по проценту хлопка.
     */
    @Query("""
            SELECT COALESCE(SUM(s.quantity), 0)
            FROM Socks s
            WHERE s.color = :color
              AND ((:operation = 'MORE_THAN' AND s.cottonPart > :cottonPart)
                OR (:operation = 'LESS_THAN' AND s.cottonPart < :cottonPart)
                OR (:operation = 'EQUAL' AND s.cottonPart = :cottonPart))
            """)
    Long sumByColorAndCottonPart(@Param("color") String color,
                                 @Param("operation") String operation,
                                 @Param("cottonPart") Integer cottonPart);
}
