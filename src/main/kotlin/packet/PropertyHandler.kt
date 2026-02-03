package com.tbread.packet

import org.slf4j.LoggerFactory
import java.io.*
import java.util.*

object PropertyHandler {
    private val props = Properties()
    private const val PROPERTIES_FILE_NAME = "settings.properties"
    private val logger = LoggerFactory.getLogger(PropertyHandler::class.java)

    init {
        loadProperties(PROPERTIES_FILE_NAME)
    }

    fun loadProperties(fname: String) {
        try {
            FileInputStream(fname).use { fis ->
                props.load(fis)
            }
        } catch (e: FileNotFoundException) {
            logger.info("Settings file not found; creating a new one.")
            FileOutputStream(fname).use {}
        } catch (e: IOException) {
            logger.error("Failed to read settings file.")
        }
    }

    private fun encodeToEucKr(key: String?): String? {
        if (key == null) return null
        return try {
            String(key.toByteArray(Charsets.ISO_8859_1), charset("EUC-KR"))
        } catch (e: UnsupportedEncodingException) {
            key
        }
    }

    private fun save(){
        FileOutputStream(PROPERTIES_FILE_NAME).use { fos ->
            props.store(fos, "settings")
        }
    }

    fun getProperty(key: String): String? {
        return encodeToEucKr(props.getProperty(key))
    }

    fun getProperty(key: String, defaultValue: String): String? {
        return encodeToEucKr(props.getProperty(key, defaultValue))
    }

    fun setProperty(key:String,value:String){
        props.setProperty(key,value)
        save()
    }


}
