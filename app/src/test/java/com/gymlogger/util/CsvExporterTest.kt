package com.gymlogger.util

import com.gymlogger.model.Meal
import com.gymlogger.model.MealItem
import com.gymlogger.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class CsvExporterTest {

    @Test
    fun testExportMealsToCsvFormatsCorrectly() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date1 = dateFormat.parse("2023-10-27 08:00")!!.time
        val date2 = dateFormat.parse("2023-10-27 13:00")!!.time

        val meals = listOf(
            Meal(
                id = 1L,
                date = date1,
                type = MealType.BREAKFAST,
                items = listOf(
                    MealItem(1L, "Eggs", "2 large"),
                    MealItem(2L, "Toast", "1 slice")
                )
            ),
            Meal(
                id = 2L,
                date = date2,
                type = MealType.LUNCH,
                items = listOf(
                    MealItem(3L, "Chicken Salad, with \"Dressing\"", "200g")
                )
            )
        )

        val csv = CsvExporter.exportMealsToCsv(meals)
        
        val expected = "Date,Time,Meal Type,Item Name,Weight\n" +
                "2023-10-27,08:00,BREAKFAST,Eggs,2 large\n" +
                "2023-10-27,08:00,BREAKFAST,Toast,1 slice\n" +
                "2023-10-27,13:00,LUNCH,\"Chicken Salad, with \"\"Dressing\"\"\",200g\n"

        assertEquals(expected, csv)
    }

    @Test
    fun testExportEmptyMealsReturnsHeaderOnly() {
        val csv = CsvExporter.exportMealsToCsv(emptyList())
        val expected = "Date,Time,Meal Type,Item Name,Weight\n"
        assertEquals(expected, csv)
    }

    @Test
    fun testExportMealWithNoItemsShowsUpWithEmptyFields() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = dateFormat.parse("2023-10-27 08:00")!!.time
        
        val meals = listOf(
            Meal(
                id = 1L,
                date = date,
                type = MealType.BREAKFAST,
                items = emptyList()
            )
        )
        
        val csv = CsvExporter.exportMealsToCsv(meals)
        val expected = "Date,Time,Meal Type,Item Name,Weight\n" +
                "2023-10-27,08:00,BREAKFAST,,\n"
        assertEquals(expected, csv)
    }
}
