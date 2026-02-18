import java.io.*

fun main(args: Array<String>) {
    val fileName = args[0]
    val lines = File(fileName).readLines()

    val types = mapOf(
        "int" to "Integer",
        "double" to "Double",
        "varchar" to "String",
        "text" to "String",
        "datetime" to "Timestamp"
    )

    println(
        """
        package com.accuscore.sim.db.ebeans.basketball;

        import javax.persistence.Column;
        import javax.persistence.Entity;
        import javax.persistence.Id;
        import javax.persistence.Table;
        import java.sql.Timestamp;

        @Entity        
    """.trimIndent()
    )

    val table = lines[0].substringAfter("create table").trim()
    val entity = table.lowercase().split("_")
        .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
    println(
        """
        @Table(name="$table")
        public class E$entity {
        
    """.trimIndent()
    )

    for (i in 2 until lines.size - 1) {
        val l = lines[i]
        try {
            val split = l.split("""\s+""".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
            val field = split[0]
            val mysqlType = split[1].substringBefore("(")
            val type = types[mysqlType] ?: mysqlType

            if (l.contains("primary key"))
                println("@Id")

            println(
                """
            @Column(name = "$field")
            private $type $field;
            
        """.trimIndent()
            )
        } catch (e: Exception) {
            println("${i+1}: $l")
            throw e
        }
    }

    println("}")

}
