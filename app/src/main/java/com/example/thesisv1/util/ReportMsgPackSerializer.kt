package com.example.thesisv1.util

import com.example.thesisv1.ViewCasualtyReportActivity
import com.fasterxml.jackson.databind.ObjectMapper
import org.msgpack.jackson.dataformat.MessagePackFactory
import java.io.File
import java.io.FileOutputStream

object ReportMsgPackSerializer {

    private val msgpackMapper = ObjectMapper(MessagePackFactory())
    private val jsonMapper = ObjectMapper()

    fun serializeToMsgPack(context: ViewCasualtyReportActivity, fileName: String, data: Map<String, Any?>): File {
        val msgPackFile = File(context.filesDir, "$fileName.msgpack")
        FileOutputStream(msgPackFile).use { output ->
            msgpackMapper.writeValue(output, data)
        }

        // Optional debug version (human-readable JSON)
        val jsonFile = File(context.filesDir, "$fileName.json")
        FileOutputStream(jsonFile).use { output ->
            output.write(jsonMapper.writeValueAsString(data).toByteArray())
        }

        return msgPackFile
    }
}
