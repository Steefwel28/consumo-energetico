package com.steefwel28.consumoenergetico.ui.components

import androidx.annotation.StringRes
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.recommender.CeilingType
import com.steefwel28.consumoenergetico.domain.recommender.HeatSource
import com.steefwel28.consumoenergetico.domain.recommender.SunExposure
import com.steefwel28.consumoenergetico.ui.screens.calculate.EquipmentCategory
import com.steefwel28.consumoenergetico.ui.screens.calculate.ManualCostMode
import com.steefwel28.consumoenergetico.ui.screens.calculate.PlateConverterMode

/**
 * Traduce las etiquetas de los enums de dominio sin meter recursos Android en
 * la capa domain. Los nombres persistidos en Room (ubicaciones, grupos) son
 * datos del usuario y no se traducen.
 */
@get:StringRes
val EquipmentCategory.labelRes: Int
    get() = when (this) {
        EquipmentCategory.AIR_CONDITIONER -> R.string.category_ac
        EquipmentCategory.REFRIGERATOR -> R.string.category_fridge
        EquipmentCategory.FREEZER -> R.string.category_freezer
        EquipmentCategory.APPLIANCES -> R.string.category_appliances
    }

@get:StringRes
val ManualCostMode.labelRes: Int
    get() = when (this) {
        ManualCostMode.DIRECT_RATE -> R.string.manual_direct
        ManualCostMode.RECEIPT -> R.string.manual_receipt
    }

@get:StringRes
val PlateConverterMode.labelRes: Int
    get() = when (this) {
        PlateConverterMode.TONS -> R.string.converter_tons
        PlateConverterMode.KILOWATTS -> R.string.converter_kw
        PlateConverterMode.AMPERES -> R.string.converter_amp
    }

@get:StringRes
val AcSchedule.labelRes: Int
    get() = when (this) {
        AcSchedule.NIGHT -> R.string.schedule_night
        AcSchedule.SUNNY_DAY -> R.string.schedule_sunny
        AcSchedule.MIXED -> R.string.schedule_mixed
    }

@get:StringRes
val SunExposure.labelRes: Int
    get() = when (this) {
        SunExposure.NONE -> R.string.sun_none
        SunExposure.CURTAIN -> R.string.sun_curtain
        SunExposure.DIRECT_AFTERNOON_SUN -> R.string.sun_direct
    }

@get:StringRes
val CeilingType.labelRes: Int
    get() = when (this) {
        CeilingType.BETWEEN_FLOORS -> R.string.ceiling_between
        CeilingType.TOP_FLOOR_SUN -> R.string.ceiling_top
        CeilingType.ZINC_OR_HIGH_CEILING -> R.string.ceiling_zinc
    }

@get:StringRes
val HeatSource.labelRes: Int
    get() = when (this) {
        HeatSource.GAMING_PC -> R.string.heat_pc
        HeatSource.OPEN_KITCHEN -> R.string.heat_kitchen
        HeatSource.DISPLAYS -> R.string.heat_displays
    }