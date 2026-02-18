import FieldType.*
import java.io.*
import java.lang.Integer.*

val replacements = mapOf(
    "End If" to "}",
    "Wend" to "}",
    "End Function" to "}",
    " Then " to ") ",
    " Then" to ") {",
    "Else" to "} else {",
    "If " to "if (",
    " And " to " && ",
    " Or " to " || ",
    "!" to ".",
    "<>" to "!=",
    " & " to " + "
)

val noSemicolonStarts = setOf("//", "/*")
val noSemicolonEnds = setOf("}", "{", "*/")

val commentStarts = setOf(
    "Option Compare Database",
    "Public Function ",
    "Recordset ",
    "Set ",
    "SysCmd "
)
val commentEnds = setOf(
    ".MoveFirst",
    ".MoveNext",
    ".Edit",
    ".Close"
)

val javaTypes = mapOf(
    "Recordset" to arrayOf("Recordset"),
    "Long" to arrayOf("long", "MapSafeI", "MI"),
    "Integer" to arrayOf("int", "MapSafeI", "MI"),
    "Double" to arrayOf("double", "MapSafeD", "MD"),
    "String" to arrayOf("String", "MapSafeS", "MS"),
    "Date" to arrayOf("Date", "MapSafeDate", "MDate")
)

val scalarInits = mapOf(
    "long" to " = 0",
    "int" to " = 0",
    "double" to " = 0.0",
    "String" to " = \"\"",
    "Date" to " = new Date()"
)

enum class FieldType {
    SCALAR,
    MAPSAFE,
    MULTIPLE2,
    MULTIPLE3,
    MULTIPLE4,
    ENTITY
}

val entityMethods = setOf(
    "RecordCount",
    "MoveFirst",
    "MoveNext",
    "addnew",
    "Edit",
    "Delete",
    "Close",
    "setsimdesc",
    "get",
    "put"
)

val fields = mutableMapOf<FieldType, MutableSet<String>>()

private fun replace(l: String, s: String, groups: MatchGroupCollection) =
    l.replaceRange(groups[0]!!.range, s)

private fun addField(type: FieldType, field: String) {
    val set = fields[type]
    if (set == null)
        fields[type] = mutableSetOf(field)
    else
        set.add(field)
}

private fun pairedParentheses(l: String, startIndex: Int): Pair<Int, String> {
    var endIndex = startIndex - 1
    var count = 1

    do {
        endIndex = l.indexOfAny(charArrayOf('(', ')'), endIndex + 1)
        if (l[endIndex] == '(') count++ else count--
    } while (count > 0)

    val key = l.substring(startIndex, endIndex)
    return Pair(endIndex, key)
}

fun equalsInIf(l: String): String? {
    val before = l.substringBefore(" Then")
    return if (before.isNotEmpty() && before.contains(" = "))
        l.replaceBefore(" Then", before.replace(" = ", " == "))
    else
        null
}

fun split(l: String): Triple<String?, String, String?> {
    if (l.isEmpty())
        return Triple(null, "", null)
    var line = l.trimStart()
    val indent = l.substringBefore(line, "")
    val apostrophes = """\w'\w""".toRegex().findAll(line)
    apostrophes.forEach {
        val i = it.range.first + 1
        val chars = line.toCharArray()
        chars[i]= '_'
        line = String(chars)
    }
    val comment = line.substringAfter("'", "")
    var content = line.substringBefore("'").trimEnd()
    apostrophes.forEach {
        val i = it.range.first + 1
        if (i < content.length) {
            val chars = content.toCharArray()
            chars[i] = '\''
            content = String(chars)
        }
    }
    return Triple(
        indent.ifEmpty { null },
        content.ifEmpty { "" },
        comment.ifEmpty { null }
    )
}

fun join(indent: String?, content: String, comment: String?): String =
    "${indent ?: ""}$content${
        if (comment != null) {
            if (content.isEmpty()) "//$comment" else " //$comment"
        } else ""
    }"

fun function(l: String): String? =
    if (l.startsWith("private ")) {
        val params = pairedParentheses(l, l.indexOf("(") + 1).second
            .split(',')
            .map {param -> param.split(' ').map {it.trim()}.filter { it.isNotEmpty() } }
        params.forEach {
            val type = it[0]
            val field = it[1]
            if (type.startsWith("MapSafe"))
                addField(MAPSAFE, field)
            else if (type.endsWith("<Key2>"))
                addField(MULTIPLE2, field)
            else if (type.endsWith("<Key3>"))
                addField(MULTIPLE3, field)
            else if (type.endsWith("<Key4>"))
                addField(MULTIPLE4, field)
        }
        l
    } else
        null

fun variable(l: String): String? =
    """Dim (?<field>\S+)(?<dimension>\(.+\))? As (?<type>\w+)"""
        .toRegex()
        .find(l)?.let { find ->
            val groups = find.groups

            val field = groups["field"]?.value!!
            val dimension = groups["dimension"]?.value
            val type = groups["type"]?.value

            val size = if (dimension == null)
                0
            else
                dimension.count { it == ',' } + 1

            val javaType = javaTypes[type]?.get(min(size, 2))

            //if java type not found then just comment the line
            if (javaType == null)
                "//$l"
            else {
                val line = when (size) {
                    0 -> "$javaType $field${scalarInits[javaType] ?: ""}"
                    1 -> "$javaType $field = new $javaType()"
                    else -> "$javaType<Key$size> $field = new $javaType<>()"
                }
                val fieldType = when (size) {
                    1 -> MAPSAFE
                    2 -> MULTIPLE2
                    3 -> MULTIPLE3
                    4 -> MULTIPLE4
                    else -> if (javaType == "Recordset") ENTITY else SCALAR
                }
                addField(fieldType, field)
                line
            }
        }

fun forLoopBegin(l: String): String? =
    """For (?<counterName>\w+) = (?<counterInitValue>.+) To (?<counterToValue>.+)"""
        .toRegex()
        .find(l)?.let { find ->
            val groups = find.groups

            val counterName = groups["counterName"]?.value
            val counterInitValue = groups["counterInitValue"]?.value
            val counterToValue = groups["counterToValue"]?.value

            replace(
                l,
                "for (int $counterName = $counterInitValue; $counterName <= $counterToValue; $counterName++) {",
                groups
            )
        }

fun forLoopEnd(l: String): String? =
    if (l.startsWith("Next "))
        "}"
    else
        null

fun whileLoopBegin(l: String): String? =
    """^While (?<condition>.+)"""
        .toRegex()
        .find(l)?.let { find ->
            val groups = find.groups
            val condition = groups["condition"]?.value!!.replace(" = ", " == ")

            replace(
                l,
                "while ($condition) {",
                groups
            )
        }

fun mapSafe(l: String, fields: Set<String>?): String =
    putOrGet(l, fields, 1)

fun multiple2(l: String, fields: Set<String>?): String =
    putOrGet(l, fields, 2)

fun multiple3(l: String, fields: Set<String>?): String =
    putOrGet(l, fields, 3)

fun multiple4(l: String, fields: Set<String>?): String =
    putOrGet(l, fields, 4)

private fun putOrGet(l: String, fields: Set<String>?, size: Int): String {
    fields?.forEach { field ->
        val start = l.indexOf("$field(")
        if (start != -1) {
            val startIndex = start + "$field(".length
            val pair = pairedParentheses(l, startIndex)
            val endIndex = pair.first
            val key = pair.second

            return if (key.isEmpty())
                putOrGet(l.replaceRange(start, endIndex + 1 , field), fields, size)
            else {
                val tail = l.substring(endIndex + 1).trimStart()
                if (tail.startsWith("= ")) {
                    val value = tail.substring(1).trimStart()

                    val replacement = when (size) {
                        1 -> "$field.put($key, $value)"
                        else -> "$field.put(new Key$size($key), $value)"
                    }
                    putOrGet(l.replaceRange(start, l.length, replacement), fields, size)
                } else {
                    val replacement = when (size) {
                        1 -> "$field.get($key)"
                        else -> "$field.get(new Key$size($key))"
                    }
                    putOrGet(l.replaceRange(start, endIndex + 1, replacement), fields, size)
                }
            }
        }
    }
    return l
}

fun putEntity(l: String, entities: Set<String>?): String? {
    entities?.forEach { entity ->
        """$entity\.(?<field>\S+) = (?<value>.+)"""
            .toRegex()
            .find(l)?.let { find ->
                val groups = find.groups
                val field = groups["field"]!!.value
                val value = groups["value"]?.value
                return replace(l, "$entity.set${field.uppercase()}($value)", groups)
            }
    }
    return null
}

fun getEntities(l: String, entities: Set<String>?): String {
    var newLine = l
    entities?.forEach { entity ->
        newLine = getEntity(newLine, entity)
    }
    return newLine
}

fun getEntity(l: String, entity: String, startIndex: Int = 0): String {
    var newLine = l
    """$entity\.(?<field>\w+)"""
        .toRegex()
        .find(l, startIndex)?.let { find ->
            val groups = find.groups
            val field = groups["field"]!!.value
            var newStartIndex = groups[0]!!.range.last
            if (field !in entityMethods && !field.startsWith("set")) {
                newLine = replace(newLine, "$entity.get${field.uppercase()}()", groups)
                newStartIndex += 5
            }
            return getEntity(newLine, entity, newStartIndex)
        }
    return newLine
}

fun saveEntity(l: String, entities: Set<String>?): String? {
    entities?.forEach { entity ->
        """$entity\.UPDATE"""
            .toRegex()
            .find(l)?.let { find ->
                val groups = find.groups
                return replace(l, "Ebean.save($entity)", groups)
            }
    }
    return null
}

fun deleteEntity(l: String, entities: Set<String>?): String? {
    entities?.forEach { entity ->
        """$entity\.Delete"""
            .toRegex()
            .find(l)?.let { find ->
                val groups = find.groups
                return replace(l, "Ebean.delete($entity)", groups)
            }
    }
    return null
}

fun comment(l: String): String? =
    if (commentStarts.map { l.startsWith(it) }.any { it }
        || commentEnds.map { l.endsWith(it) }.any { it }
    )
        "//$l"
    else
        null

fun semicolon(l: String): String? =
    if (l.isEmpty()
        || noSemicolonStarts.map { l.trimStart().startsWith(it) }.any { it }
        || noSemicolonEnds.map { l.endsWith(it) }.any { it }
    )
        null
    else
        "$l;"

fun convert(lines: List<String>, printFields: Boolean = false, noGetters: Boolean = false): List<String> {
    val newLines = lines.map { line ->
        try {
            var (indent, content, comment) = split(line.trimEnd())

            if (content.isNotEmpty()) {
                //replace single = to double == inside if expression
                equalsInIf(content)?.let { content = it }

                //straight replacement of some simple expressions
                replacements.forEach {
                    content = content.replace(it.key, it.value)
                }

                //variable definition from function
                function(content)?.let { content = it }

                //variable definition from body
                variable(content)?.let { content = it }

                //for loop
                forLoopBegin(content)?.let { content = it }
                forLoopEnd(content)?.let { content = it }

                //while loop
                whileLoopBegin(content)?.let { content = it }

                //put values into Multiple2
                content = multiple2(content, fields[MULTIPLE2])

                //put values into Multiple3
                content = multiple3(content, fields[MULTIPLE3])

                //put values into Multiple4
                content = multiple4(content, fields[MULTIPLE4])

                //put values into MapSafe
                content = mapSafe(content, fields[MAPSAFE])

                //set fields of entities
                putEntity(content, fields[ENTITY])?.let { content = it }

                //save entities
                saveEntity(content, fields[ENTITY])?.let { content = it }

                //delete entities
                deleteEntity(content, fields[ENTITY])?.let { content = it }

                //use getters for entities
                if (!noGetters)
                    content = getEntities(content, fields[ENTITY])

                //comment useless lines
                comment(content)?.let { content = it }

                //close statements with semicolon if required
                semicolon(content)?.let { content = it }
            }
            join(indent, content, comment)
        } catch (e: Exception) {
            println(line)
            throw e
        }
    }

    if (printFields)
        fields.forEach {
            println("${it.key}=${it.value}")
        }

    return newLines
}

fun main(args: Array<String>) {
    val fileName = args[0]
    val lines = File(fileName).readLines()

    val newLines = convert(lines, printFields = args.contains("fields"), noGetters = args.contains("nogetters"))

    val text = newLines.joinToString("\n")

    if (!args.contains("noconsole"))
        println(text)

    if (args.contains("file")) {
        val name = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.')
        val newFileName = "$name-java.$ext"
        File(newFileName).writeText(text)
    }
}
