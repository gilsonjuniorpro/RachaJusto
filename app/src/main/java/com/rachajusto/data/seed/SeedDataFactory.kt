package com.rachajusto.data.seed

import com.rachajusto.domain.engine.SplitCalculator
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.ExpenseLineItem
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.model.SplitMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object SeedDataFactory {

    fun createSeedGroups(referenceDate: LocalDate = LocalDate.now()): List<Group> {
        return listOf(
            createCamposDoJordaoGroup(referenceDate),
            createRepublicaGroup(referenceDate),
            createBirthdayBbqGroup(referenceDate)
        )
    }

    private fun createCamposDoJordaoGroup(today: LocalDate): Group {
        val groupId = "grp_campos_do_jordao"
        val lucas = Participant("p_lucas", "Lucas", 0xFF0F766E, "lucas.mendes@pixdemo.br", "SAO PAULO")
        val ana = Participant("p_ana", "Ana", 0xFF2563EB, "ana.oliveira@pixdemo.br", "SAO PAULO")
        val pedro = Participant("p_pedro", "Pedro", 0xFF7C3AED, "11987654321", "CAMPINAS")
        val carla = Participant("p_carla", "Carla", 0xFFDB2777, "carla.souza@pixdemo.br", "SAO PAULO")
        val rafa = Participant("p_rafa", "Rafa", 0xFFEA580C, "rafa.costa@pixdemo.br", "SANTOS")
        val bia = Participant("p_bia", "Bia", 0xFF0891B2, "bia.ferreira@pixdemo.br", "SAO PAULO")

        val allParticipants = listOf(lucas, ana, pedro, carla, rafa, bia)
        val allIds = allParticipants.map { it.id }
        val drinkersIds = listOf(lucas.id, pedro.id, carla.id, rafa.id)
        val car1Ids = listOf(lucas.id, rafa.id, bia.id)

        val fondueItems = listOf(
            ExpenseLineItem(
                id = "item_fondue_1",
                name = "Fondue de Queijo (2x)",
                amountCents = 21000L,
                participantIds = allIds
            ),
            ExpenseLineItem(
                id = "item_fondue_2",
                name = "Fondue de Filé Mignon",
                amountCents = 19500L,
                participantIds = listOf(lucas.id, pedro.id, carla.id, rafa.id, bia.id)
            ),
            ExpenseLineItem(
                id = "item_fondue_3",
                name = "Fondue de Chocolate Belga",
                amountCents = 12000L,
                participantIds = listOf(ana.id, bia.id, carla.id, lucas.id)
            ),
            ExpenseLineItem(
                id = "item_fondue_4",
                name = "Vinho Tinto Reserva (2 garrafas)",
                amountCents = 16000L,
                participantIds = drinkersIds
            )
        )
        val fondueCalc = SplitCalculator.calculateItemizedSplits(
            items = fondueItems,
            serviceFeePercent = 10
        )

        val expenses = listOf(
            Expense(
                id = "exp_cj_1",
                groupId = groupId,
                title = "Airbnb Chalé",
                category = ExpenseCategory.LODGING,
                date = today.minusDays(4),
                paidById = lucas.id,
                totalAmountCents = 240000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(240000L, allIds),
                createdAtIso = isoAt(today.minusDays(4), 10)
            ),
            Expense(
                id = "exp_cj_2",
                groupId = groupId,
                title = "Mercado + Carnes",
                category = ExpenseCategory.MARKET,
                date = today.minusDays(3),
                paidById = ana.id,
                totalAmountCents = 68000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(68000L, allIds),
                createdAtIso = isoAt(today.minusDays(3), 14)
            ),
            Expense(
                id = "exp_cj_3",
                groupId = groupId,
                title = "Cervejas e Vinhos",
                category = ExpenseCategory.DRINKS,
                date = today.minusDays(3),
                paidById = pedro.id,
                totalAmountCents = 42000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(42000L, drinkersIds),
                createdAtIso = isoAt(today.minusDays(3), 16)
            ),
            Expense(
                id = "exp_cj_4",
                groupId = groupId,
                title = "Restaurante Fondue",
                category = ExpenseCategory.FOOD,
                date = today.minusDays(2),
                paidById = carla.id,
                totalAmountCents = fondueCalc.grandTotalCents,
                serviceFeePercent = 10,
                splitMode = SplitMode.ITEMIZED,
                items = fondueItems,
                splits = fondueCalc.splits,
                createdAtIso = isoAt(today.minusDays(2), 21)
            ),
            Expense(
                id = "exp_cj_5",
                groupId = groupId,
                title = "Gasolina Carro 1",
                category = ExpenseCategory.TRANSPORT,
                date = today.minusDays(2),
                paidById = rafa.id,
                totalAmountCents = 24000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(24000L, car1Ids),
                createdAtIso = isoAt(today.minusDays(2), 9)
            ),
            Expense(
                id = "exp_cj_6",
                groupId = groupId,
                title = "Pedágios",
                category = ExpenseCategory.TRANSPORT,
                date = today.minusDays(1),
                paidById = lucas.id,
                totalAmountCents = 8650L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(8650L, allIds),
                createdAtIso = isoAt(today.minusDays(1), 17)
            ),
            Expense(
                id = "exp_cj_7",
                groupId = groupId,
                title = "Café da Manhã Padaria",
                category = ExpenseCategory.FOOD,
                date = today.minusDays(1),
                paidById = bia.id,
                totalAmountCents = 15400L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(15400L, allIds),
                createdAtIso = isoAt(today.minusDays(1), 8)
            )
        )

        val partialSettlement = SettlementPayment(
            id = "stl_cj_1",
            groupId = groupId,
            fromParticipantId = rafa.id,
            toParticipantId = lucas.id,
            amountCents = 20000L,
            paidAtIso = isoAt(today, 11),
            note = "PIX adiantado do chalé"
        )

        return Group(
            id = groupId,
            name = "Fim de Semana em Campos do Jordão",
            description = "Chalé na serra, fondue, churrasco e estrada com a turma",
            emoji = "🏔️",
            participants = allParticipants,
            expenses = expenses,
            settlements = listOf(partialSettlement),
            createdAtIso = isoAt(today.minusDays(5), 9)
        )
    }

    private fun createRepublicaGroup(today: LocalDate): Group {
        val groupId = "grp_republica_402"
        val gabriel = Participant("p_gabriel", "Gabriel", 0xFF0F766E, "gabriel.lima@pixdemo.br", "SAO PAULO")
        val matheus = Participant("p_matheus", "Matheus", 0xFF2563EB, "matheus.rocha@pixdemo.br", "SAO PAULO")
        val tiago = Participant("p_tiago", "Tiago", 0xFFD97706, "tiago.alves@pixdemo.br", "SAO PAULO")

        val allParticipants = listOf(gabriel, matheus, tiago)
        val allIds = allParticipants.map { it.id }

        val expenses = listOf(
            Expense(
                id = "exp_rep_1",
                groupId = groupId,
                title = "Aluguel",
                category = ExpenseCategory.LODGING,
                date = today.minusDays(6),
                paidById = gabriel.id,
                totalAmountCents = 360000L,
                splitMode = SplitMode.SHARES,
                splits = SplitCalculator.calculateShareSplits(
                    totalCents = 360000L,
                    weightsByParticipant = mapOf(
                        gabriel.id to 3,
                        matheus.id to 2,
                        tiago.id to 2
                    )
                ),
                createdAtIso = isoAt(today.minusDays(6), 10)
            ),
            Expense(
                id = "exp_rep_2",
                groupId = groupId,
                title = "Energia Elétrica",
                category = ExpenseCategory.OTHER,
                date = today.minusDays(4),
                paidById = matheus.id,
                totalAmountCents = 31245L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(31245L, allIds),
                createdAtIso = isoAt(today.minusDays(4), 15)
            ),
            Expense(
                id = "exp_rep_3",
                groupId = groupId,
                title = "Internet Fibra",
                category = ExpenseCategory.OTHER,
                date = today.minusDays(3),
                paidById = tiago.id,
                totalAmountCents = 13990L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(13990L, allIds),
                createdAtIso = isoAt(today.minusDays(3), 12)
            ),
            Expense(
                id = "exp_rep_4",
                groupId = groupId,
                title = "Faxina Quinzenal",
                category = ExpenseCategory.OTHER,
                date = today.minusDays(1),
                paidById = gabriel.id,
                totalAmountCents = 32000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(32000L, allIds),
                createdAtIso = isoAt(today.minusDays(1), 16)
            )
        )

        return Group(
            id = groupId,
            name = "República Apê 402 — Contas do Mês",
            description = "Aluguel proporcional por quarto e contas fixas mensais",
            emoji = "🏠",
            participants = allParticipants,
            expenses = expenses,
            settlements = emptyList(),
            createdAtIso = isoAt(today.minusDays(7), 8)
        )
    }

    private fun createBirthdayBbqGroup(today: LocalDate): Group {
        val groupId = "grp_churrasco_niver"
        val bruno = Participant("p_bruno", "Bruno", 0xFFEA580C, "bruno.silva@pixdemo.br", "CURITIBA")
        val camila = Participant("p_camila", "Camila", 0xFFDB2777, "camila.dias@pixdemo.br", "CURITIBA")
        val diego = Participant("p_diego", "Diego", 0xFF0891B2, "41998877665", "CURITIBA")
        val fernanda = Participant("p_fernanda", "Fernanda", 0xFF65A30D, "fernanda.luz@pixdemo.br", "CURITIBA")

        val participants = listOf(bruno, camila, diego, fernanda)
        val allIds = participants.map { it.id }

        val meatExpense = Expense(
            id = "exp_bbq_1",
            groupId = groupId,
            title = "Carnes e Carvão",
            category = ExpenseCategory.MARKET,
            date = today.minusDays(10),
            paidById = bruno.id,
            totalAmountCents = 40000L,
            splitMode = SplitMode.EQUAL,
            splits = SplitCalculator.calculateEqualSplits(40000L, allIds),
            createdAtIso = isoAt(today.minusDays(10), 11)
        )
        val drinksExpense = Expense(
            id = "exp_bbq_2",
            groupId = groupId,
            title = "Bebidas e Sobremesa",
            category = ExpenseCategory.DRINKS,
            date = today.minusDays(10),
            paidById = camila.id,
            totalAmountCents = 20000L,
            splitMode = SplitMode.EXACT,
            splits = SplitCalculator.calculateExactSplits(
                totalCents = 20000L,
                amountsByParticipant = mapOf(
                    bruno.id to 5000L,
                    camila.id to 5000L,
                    diego.id to 6000L,
                    fernanda.id to 4000L
                )
            ),
            createdAtIso = isoAt(today.minusDays(10), 12)
        )

        // Balances before settlements: Bruno=+250, Camila=+50, Diego=-160, Fernanda=-140
        // Fully settled in 3 payments so this group demonstrates 100% settled state!
        val settlements = listOf(
            SettlementPayment(
                id = "stl_bbq_1",
                groupId = groupId,
                fromParticipantId = diego.id,
                toParticipantId = bruno.id,
                amountCents = 16000L,
                paidAtIso = isoAt(today.minusDays(9), 18),
                note = "PIX"
            ),
            SettlementPayment(
                id = "stl_bbq_2",
                groupId = groupId,
                fromParticipantId = fernanda.id,
                toParticipantId = bruno.id,
                amountCents = 9000L,
                paidAtIso = isoAt(today.minusDays(9), 19),
                note = "PIX"
            ),
            SettlementPayment(
                id = "stl_bbq_3",
                groupId = groupId,
                fromParticipantId = fernanda.id,
                toParticipantId = camila.id,
                amountCents = 5000L,
                paidAtIso = isoAt(today.minusDays(9), 19),
                note = "PIX"
            )
        )

        return Group(
            id = groupId,
            name = "Churrasco de Aniversário",
            description = "Confraternização de domingo já 100% acertada no PIX",
            emoji = "🍖",
            participants = participants,
            expenses = listOf(meatExpense, drinksExpense),
            settlements = settlements,
            createdAtIso = isoAt(today.minusDays(11), 9)
        )
    }

    private fun isoAt(date: LocalDate, hour: Int): String {
        return Instant.from(date.atTime(hour, 0).atOffset(ZoneOffset.UTC)).toString()
    }
}
