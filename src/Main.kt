import java.io.File

fun main(args: Array<String>) {
	val pointScale = if (args.size > 1) {
		when (args[1]) {
			"2" -> PointScale.TWO_POINT
			"5" -> PointScale.FIVE_POINT
			else -> PointScale.TEN_POINT
		}
	}else
		PointScale.TEN_POINT


	if(args.isEmpty())
		return

	val lines = File(args[0]).readLines() as MutableList<String>

	var tableBufferIndex = 0
	val tableBuffer = ArrayList<ArrayList<String>>()
	tableBuffer.add(ArrayList())
	for (line in lines) {
		if (line.isEmpty()) {
			tableBufferIndex++
			tableBuffer.add(ArrayList())
			continue
		}
		tableBuffer[tableBufferIndex].add(line)
	}


	class Table(buf: ArrayList<String>) {
		val tableNumLine = buf[0]
		val shortLabel = buf[3].substring(2)
		val titleLines = ArrayList<String>(buf.filter { l -> l.startsWith("T ") })
		val rows = ArrayList<String>(buf.filter { l -> l.startsWith('R') })
		val position : String
		val qualifier : String
		val netRowFirstPart = "R $shortLabel;"
		val netRowSecondPart : String

		init {
			//replace quotes
			titleLines.map { toASCII(it) }

			when (pointScale) {
				PointScale.TEN_POINT -> {
					position = rows[0].substring(rows[0].lastIndexOf(';') + 4, rows[0].lastIndexOf(','))

					rows.add(0, "R NET: 7-10; r($position,07:10)")
					rows.add(0, "R NET: 5-6;  r($position,05:06)")
					rows.add(0, "R NET: 1-4;  r($position,01:04)")

					qualifier = "Q r($position,01:99)"

					netRowSecondPart = "r($position,07:10); vbase "

					rows.add("R ; null")
					rows.add("R Mean; a($position),r($position,01:10); fdp 1 freq novp")
				}
				PointScale.FIVE_POINT -> {
					position = rows[0].substring(rows[0].lastIndexOf(';') + 2, rows[0].lastIndexOf('-'))

					rows.add(0, "R BOT2; $position-4,5")
					rows.add(0, "R TOP2; $position-1,2")

					qualifier = "Q $position-1:9"

					netRowSecondPart = "$position-1:2; vbase "

					rows.add("R ; null")
					rows.add("R Mean; a($position),$position-1:5; fdp 1 freq novp")
				}
				PointScale.TWO_POINT -> {
					position = rows[0].substring(rows[0].lastIndexOf(';') + 2, rows[0].lastIndexOf('-'))

					qualifier = "Q $position-1:9"

					netRowSecondPart = "$position-1; vbase "
				}
			}
		}

		override fun toString(): String {
			val buf = StringBuilder("$tableNumLine\n")
			titleLines.forEach { l -> buf.append("$l\n") }
			buf.append("$qualifier\n")
			rows.forEach { r -> buf.append("$r\n") }
			return buf.toString()
		}
	}

	val tables = ArrayList<Table>()
	tableBuffer.mapTo(tables) { Table(it) }

	val name = tables[0].titleLines[1].split(' ')[1].dropLast(1)
	val netTable = ArrayList<String>()
	netTable.add("TABLE ")

	when (pointScale) {
		PointScale.TEN_POINT    -> netTable.add("T $name Summary of 7 - 10 scores")
		PointScale.FIVE_POINT   -> netTable.add("T $name Summary of 1 - 2 scores")
		PointScale.TWO_POINT    -> netTable.add("T $name Summary of Yes")
	}

	netTable.add("T &wt Q" + name + "_SUMMARY")
	netTable.add("O rankvpct")
	netTable.add("Q (1'D1XX')")

	if (pointScale == PointScale.TEN_POINT)
		tables.forEach { t -> netTable.add("R null; r(" + t.position + ",01:99); noprint nor") }
	else
		tables.forEach { t -> netTable.add("R null; " + t.position + "-1:9; noprint nor") }


	var maxLineLength = 0
	tables.forEach { t ->
		val newLength = t.netRowFirstPart.length
		if (newLength > maxLineLength)
			maxLineLength = newLength
	}

	//the width target is the length of netRowFirstPart plus two tabs
	val widthTarget = maxLineLength + (4 - maxLineLength % 4) + 4     //plus (4 - max % 4) to get the length of one tab, plus 4 so that there is one more tab

	var vbaseNum = 2
	tables.forEach { t ->
		val tabsNeeded = (widthTarget - t.netRowFirstPart.length + 3) / 4
		val tabs = getTabs(tabsNeeded)

		netTable.add(t.netRowFirstPart + tabs + t.netRowSecondPart + vbaseNum++)
	}

	File("ans.txt").printWriter().use {out ->
		tables.forEach{
			out.println(it)
		}
		netTable.forEach {
			out.println(it)
		}
	}
}

fun getTabs(x: Int): String {
	return String(CharArray(x){ _ -> '\t'})
}

fun toASCII(s: String): String {
	var newString = s
	newString = newString.replace("``", "\"")
	newString = newString.replace("´´", "\"")
	newString = newString.replace("´", "'")
	newString = newString.replace("’", "'")

	return newString
}

enum class PointScale{
	TEN_POINT,
	FIVE_POINT,
	TWO_POINT
}