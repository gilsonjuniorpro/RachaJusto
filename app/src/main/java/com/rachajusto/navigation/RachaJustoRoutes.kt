package com.rachajusto.navigation

object RachaJustoRoutes {
    const val GROUPS = "groups"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val ADD_EXPENSE = "add_expense/{groupId}"

    fun groupDetail(groupId: String): String = "group_detail/$groupId"
    fun addExpense(groupId: String): String = "add_expense/$groupId"
}
