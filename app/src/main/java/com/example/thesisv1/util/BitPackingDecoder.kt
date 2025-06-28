package com.example.thesisv1.util

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BitPackingDecoder {

    fun decodeCasualtyReportBin(file: File): String {
        val buffer = file.readBytes().wrapBig()

        val reportId = buffer.readPrefixedString()
        val profileId = buffer.readPrefixedString()

        val affectedFamilies = buffer.uByte()
        val affectedPersons = buffer.uByte()
        val age0to2 = buffer.uByte()
        val age3to5 = buffer.uByte()
        val age6to12 = buffer.uByte()
        val age13to17 = buffer.uByte()
        val pwdCount = buffer.uByte()
        val elderlyCount = buffer.uByte()

        val missingMale = buffer.uByte()
        val missingFemale = buffer.uByte()
        val injuredMale = buffer.uByte()
        val injuredFemale = buffer.uByte()
        val deadMale = buffer.uByte()
        val deadFemale = buffer.uByte()

        val flags = buffer.get().toInt()
        val needResources = (flags and 0b10000000) != 0
        val foodFlag = (flags and 0b00010000) != 0
        val nonFoodFlag = (flags and 0b00001000) != 0
        val medicalFlag = (flags and 0b00000100) != 0
        val shelterFlag = (flags and 0b00000010) != 0
        val manpowerFlag = (flags and 0b00000001) != 0

        val priority = buffer.uByte()
        val latitude = buffer.readFloatLE()
        val longitude = buffer.readFloatLE()

        val food = buffer.readPrefixedString()
        val nonFood = buffer.readPrefixedString()
        val medical = buffer.readPrefixedString()
        val shelter = buffer.readPrefixedString()
        val manpower = buffer.readPrefixedString()

        return """
            ✅ Decoded Casualty Report

            Report ID: $reportId
            Disaster Profile ID: $profileId

            Affected: $affectedFamilies families, $affectedPersons persons
            Ages: 0–2: $age0to2, 3–5: $age3to5, 6–12: $age6to12, 13–17: $age13to17
            PWD: $pwdCount, Elderly: $elderlyCount

            Missing: $missingMale M, $missingFemale F
            Injured: $injuredMale M, $injuredFemale F
            Dead: $deadMale M, $deadFemale F

            Needs Resources: ${if (needResources) "Yes" else "No"}
            Priority Level: $priority

            ➤ Food: ${if (foodFlag) food else "None"}
            ➤ Non-Food: ${if (nonFoodFlag) nonFood else "None"}
            ➤ Medical: ${if (medicalFlag) medical else "None"}
            ➤ Shelter: ${if (shelterFlag) shelter else "None"}
            ➤ Manpower: ${if (manpowerFlag) manpower else "None"}

            Location: $latitude, $longitude
        """.trimIndent()
    }

    fun decodeInfrastructureReportBin(file: File): String {
        return try {
            val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)

            val reportId = readLengthPrefixedString(buffer)
            val profileId = readLengthPrefixedString(buffer)

            val facilityType = buffer.get().toUByte().toInt()
            val damageType = buffer.get().toUByte().toInt()

            val remarks = readLengthPrefixedString(buffer)

            val latitude = buffer.order(ByteOrder.LITTLE_ENDIAN).float
            val longitude = buffer.order(ByteOrder.LITTLE_ENDIAN).float
            buffer.order(ByteOrder.BIG_ENDIAN) // Restore just in case

            val facilityMap = mapOf(
                1 to "Residential House", 2 to "Commercial Building", 3 to "School",
                4 to "Hospitals", 5 to "Government Buildings", 6 to "Other Public Infrastructure"
            )

            val damageMap = mapOf(
                1 to "Partially Damaged", 2 to "Totally Damaged"
            )

            """
        ✅ Decoded Infrastructure Report

        Report ID: $reportId
        Disaster Profile ID: $profileId

        Facility Type: ${facilityMap[facilityType] ?: "Unknown"}
        Damage Type: ${damageMap[damageType] ?: "Unknown"}
        Remarks: ${remarks.ifBlank { "None" }}

        Location: $latitude, $longitude
        """.trimIndent()
        } catch (e: Exception) {
            "❌ Error decoding Infrastructure Report: ${e.message}"
        }
    }


    fun decodeAccessibilityReportBin(file: File): String {
        return try {
            val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)

            val reportId = readLengthPrefixedString(buffer)
            val profileId = readLengthPrefixedString(buffer)

            val facilityType = buffer.get().toUByte().toInt()
            val status = buffer.get().toUByte().toInt()

            val obstructionCount = buffer.get().toUByte().toInt()
            val obstructions = (0 until obstructionCount).map { buffer.get().toUByte().toInt() }

            val otherObstruction = readLengthPrefixedString(buffer)

            val alternativeRouteAvailable = buffer.get().toInt() == 1
            val altRouteDescription = readLengthPrefixedString(buffer)
            val remarks = readLengthPrefixedString(buffer)
            val assessedBy = readLengthPrefixedString(buffer)

            val latitude = buffer.order(ByteOrder.LITTLE_ENDIAN).float
            val longitude = buffer.order(ByteOrder.LITTLE_ENDIAN).float
            buffer.order(ByteOrder.BIG_ENDIAN)

            val facilityMap = mapOf(
                1 to "Main Road", 2 to "National Road", 3 to "Access Road to Affected Area",
                4 to "Bridge"
            )

            val statusMap = mapOf(
                1 to "Passable to All Vehicles", 2 to "Passable to Light Vehicles Only", 3 to "Passable by Foot Only", 4 to "Not Passable", 6 to "Unknown"
            )

            val obstructionMap = mapOf(
                1 to "Flooding", 2 to "Landslide", 3 to "Fallen Trees",
                4 to "Debris", 5 to "Damaged Bridge/Infrastructure", 6 to "Other"
            )

            val obstructionDescriptions = obstructions.mapNotNull { obstructionMap[it] }

            """
        ✅ Decoded Accessibility Report

        Report ID: $reportId
        Disaster Profile ID: $profileId

        Facility Type: ${facilityMap[facilityType] ?: "Unknown"}
        Status: ${statusMap[status] ?: "Unknown"}

        Obstructions: ${obstructionDescriptions.joinToString(", ").ifBlank { "None" }}
        Other Obstruction: ${otherObstruction.ifBlank { "None" }}

        Alternative Route Available: ${if (alternativeRouteAvailable) "Yes" else "No"}
        Description: ${altRouteDescription.ifBlank { "None" }}

        Remarks: ${remarks.ifBlank { "None" }}
        Assessed By: ${assessedBy.ifBlank { "None" }}

        Location: $latitude, $longitude
        """.trimIndent()
        } catch (e: Exception) {
            "❌ Error decoding Accessibility Report: ${e.message}"
        }
    }


    fun decodeUtilityReportBin(file: File): String {
        val buffer = file.readBytes().wrapBig()
        val reportId = buffer.readPrefixedString()
        val profileId = buffer.readPrefixedString()
        val utilityType = buffer.uByte()
        val status = buffer.uByte()
        val details = buffer.readPrefixedString()
        val otherDetail = buffer.readPrefixedString()
        val remarks = buffer.readPrefixedString()
        val latitude = buffer.readFloatLE()
        val longitude = buffer.readFloatLE()

        return """
            ⚡ Utility Disruption Report

            Report ID: $reportId
            Disaster Profile ID: $profileId

            Utility Type (Tag): $utilityType
            Disruption Status (Tag): $status
            Disruption Details: ${details.ifBlank { "None" }}
            Other Details: ${otherDetail.ifBlank { "None" }}
            Remarks: ${remarks.ifBlank { "None" }}

            Location: $latitude, $longitude
        """.trimIndent()
    }

    // ──────── 🔧 Utility Extension Methods ────────

    private fun ByteArray.wrapBig(): ByteBuffer = ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN)

    private fun ByteBuffer.readPrefixedString(): String {
        val len = this.get().toInt() and 0xFF
        val bytes = ByteArray(len)
        this.get(bytes)
        return String(bytes)
    }

    private fun ByteBuffer.uByte(): Int = this.get().toUByte().toInt()

    private fun ByteBuffer.readFloatLE(): Float {
        val bytes = ByteArray(4)
        this.get(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
    }

    private fun readLengthPrefixedString(buffer: ByteBuffer): String {
        val len = buffer.get().toInt() and 0xFF
        val bytes = ByteArray(len)
        buffer.get(bytes)
        return String(bytes)
    }
}
