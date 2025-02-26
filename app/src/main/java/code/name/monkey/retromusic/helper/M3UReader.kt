package code.name.monkey.retromusic.helper

import code.name.monkey.retromusic.helper.M3UConstants.ENTRY
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

object M3UReader : M3UConstants {
    @JvmStatic
    @Throws(IOException::class)
    fun read(m3uFile: File): List<String> {
        require(m3uFile.exists()) { "Input file does not exist" }

        return BufferedReader(FileReader(m3uFile)).use { reader ->
            reader.readLine()

            reader.lineSequence()
                .filter { !it.startsWith("#") }
                .toList()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readIO(input: InputStream): List<String> {
        requireNotNull(input) { "Input stream cannot be null" }

        return BufferedReader(InputStreamReader(input)).use { reader ->
            reader.readLine()

            reader.lineSequence()
                .filterNot { line ->
                    line.startsWith(ENTRY)
                }
                .toList()
        }
    }

    @JvmStatic
    fun readIO(inputReader: Reader): List<String> {
        BufferedReader(inputReader).use { reader ->
            reader.readLine()

            return reader.lineSequence()
                .filterNot { line -> line.startsWith("#") }
                .toList()
        }
    }
}
