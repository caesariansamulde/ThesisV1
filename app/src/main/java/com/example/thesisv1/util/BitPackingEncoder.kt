package com.example.thesisv1.util

import com.example.thesisv1.data.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BitPackingEncoder {

    // --- CASUALTY REPORT ---
    fun encodeCasualtyReport(report: CasualtyReport): ByteArray {
        val buffer = ByteArrayOutputStream()

        writeLengthPrefixedString(buffer, report.reportId)
        writeLengthPrefixedString(buffer, report.disasterProfileId)

        buffer.write(report.affectedFamilies and 0xFF)
        buffer.write(report.affectedPersons and 0xFF)
        buffer.write(report.age0to2 and 0xFF)
        buffer.write(report.age3to5 and 0xFF)
        buffer.write(report.age6to12 and 0xFF)
        buffer.write(report.age13to17 and 0xFF)
        buffer.write(report.pwdCount and 0xFF)
        buffer.write(report.elderlyCount and 0xFF)
        buffer.write(report.missingMale and 0xFF)
        buffer.write(report.missingFemale and 0xFF)
        buffer.write(report.injuredMale and 0xFF)
        buffer.write(report.injuredFemale and 0xFF)
        buffer.write(report.deadMale and 0xFF)
        buffer.write(report.deadFemale and 0xFF)

        var resourceFlags = 0
        if (report.needResources) resourceFlags = resourceFlags or (1 shl 7)
        if (!report.food.isNullOrEmpty()) resourceFlags = resourceFlags or (1 shl 4)
        if (!report.nonFood.isNullOrEmpty()) resourceFlags = resourceFlags or (1 shl 3)
        if (!report.medical.isNullOrEmpty()) resourceFlags = resourceFlags or (1 shl 2)
        if (!report.shelter.isNullOrEmpty()) resourceFlags = resourceFlags or (1 shl 1)
        if (!report.manpower.isNullOrEmpty()) resourceFlags = resourceFlags or 1
        buffer.write(resourceFlags)

        buffer.write((report.priority?.toIntOrNull() ?: 0) and 0xFF)
        buffer.write(floatToBytesLE(report.latitude.toFloat()))
        buffer.write(floatToBytesLE(report.longitude.toFloat()))

        writeLengthPrefixedString(buffer, report.food)
        writeLengthPrefixedString(buffer, report.nonFood)
        writeLengthPrefixedString(buffer, report.medical)
        writeLengthPrefixedString(buffer, report.shelter)
        writeLengthPrefixedString(buffer, report.manpower)

        return buffer.toByteArray()
    }

    // --- INFRASTRUCTURE REPORT ---
    fun encodeInfrastructureReport(report: InfrastructureReport): ByteArray {
        val buffer = ByteArrayOutputStream()

        writeLengthPrefixedString(buffer, report.reportId)
        writeLengthPrefixedString(buffer, report.disasterProfileId)

        buffer.write(report.facilityType and 0xFF)
        buffer.write(report.damage and 0xFF)

        writeLengthPrefixedString(buffer, report.remarks)
        buffer.write(floatToBytesLE(report.latitude.toFloat()))
        buffer.write(floatToBytesLE(report.longitude.toFloat()))

        return buffer.toByteArray()
    }

    // --- ACCESSIBILITY REPORT ---
    fun encodeAccessibilityReport(report: AccessibilityReport): ByteArray {
        val buffer = ByteArrayOutputStream()

        writeLengthPrefixedString(buffer, report.reportId)
        writeLengthPrefixedString(buffer, report.disasterProfileId)

        buffer.write(report.facilityType and 0xFF)
        buffer.write(report.status and 0xFF)

        // Obstructions: store count and each obstruction int
        buffer.write(report.obstructions.size and 0xFF)
        report.obstructions.forEach { buffer.write(it and 0xFF) }

        writeLengthPrefixedString(buffer, report.otherObstruction)
        buffer.write(if (report.alternativeRouteAvailable) 1 else 0)
        writeLengthPrefixedString(buffer, report.alternativeRouteDescription)
        writeLengthPrefixedString(buffer, report.remarks)
        writeLengthPrefixedString(buffer, report.assessedBy)
        buffer.write(floatToBytesLE(report.latitude.toFloat()))
        buffer.write(floatToBytesLE(report.longitude.toFloat()))

        return buffer.toByteArray()
    }

    // --- UTILITY DISRUPTION REPORT ---
    fun encodeUtilityDisruptionReport(report: UtilityDisruptionReport): ByteArray {
        val buffer = ByteArrayOutputStream()

        writeLengthPrefixedString(buffer, report.reportId)
        writeLengthPrefixedString(buffer, report.disasterProfileId)

        buffer.write(report.utilityType and 0xFF)
        buffer.write(report.status and 0xFF)

        writeLengthPrefixedString(buffer, report.disruptionDetails)
        writeLengthPrefixedString(buffer, report.otherDetail)
        writeLengthPrefixedString(buffer, report.remarks)

        buffer.write(floatToBytesLE(report.latitude.toFloat()))
        buffer.write(floatToBytesLE(report.longitude.toFloat()))

        return buffer.toByteArray()
    }

    // --- HELPERS ---
    private fun writeLengthPrefixedString(buffer: ByteArrayOutputStream, value: String?) {
        val bytes = value?.toByteArray(Charsets.UTF_8) ?: byteArrayOf()
        buffer.write(bytes.size and 0xFF)
        buffer.write(bytes)
    }

    private fun floatToBytesLE(value: Float): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
    }
}
