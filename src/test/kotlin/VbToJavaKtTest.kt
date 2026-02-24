import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

internal class VbToJavaKtTest {

    @Test
    fun split() {
        val (indent1, content1, comment1) = split("")
        assertNull(indent1)
        assertEquals("", content1)
        assertNull(comment1)

        val (indent2, content2, comment2) = split("        defcoord(TEAMID.ID) = TEAMID.DEFFG '- (TEAMID.adj07) * 0.05 'TEAMID.YCOORD '+ TEAMID.adj06")
        assertEquals("        ", indent2)
        assertEquals("defcoord(TEAMID.ID) = TEAMID.DEFFG", content2)
        assertEquals("- (TEAMID.adj07) * 0.05 'TEAMID.YCOORD '+ TEAMID.adj06", comment2)

        val (indent3, content3, comment3) = split("Dim count As Long")
        assertNull(indent3)
        assertEquals("Dim count As Long", content3)
        assertNull(comment3)

        val (indent4, content4, comment4) = split("'if (season == 0) {")
        assertNull(indent4)
        assertEquals("", content4)
        assertEquals("if (season == 0) {", comment4)

        val (indent5, content5, comment5) = split("pickstring = \" Over-Under Line is \" & sched!OU_M")
        assertNull(indent5)
        assertEquals("pickstring = \" Over-Under Line is \" & sched!OU_M", content5)
        assertNull(comment5)

    }

    @Test
    fun join() {
        assertEquals(
            "",
            join(null, "", null)
        )
        assertEquals(
            "just content",
            join(null, "just content", null)
        )
        assertEquals(
            "//just comment",
            join(null, "", "just comment")
        )
        assertEquals(
            "the content //and comment",
            join(null, "the content", "and comment")
        )
        assertEquals(
            "    the content with indent //and comment",
            join("    ", "the content with indent", "and comment")
        )
    }

    @Test
    fun function() {
        assertEquals("private double startertipadj(int playercount, MapSafeD otip, MapSafeI odepth, int startingtiptotal) {",
            function("private double startertipadj(int playercount, MapSafeD otip, MapSafeI odepth, int startingtiptotal) {"))
        assertNull(
            function("Dim count As Long"))
    }

    @Test
    fun variable() {
        assertEquals("long count = 0",
            variable("Dim count As Long"))
        assertEquals("MapSafeD ADJMIN = new MapSafeD()",
            variable("Dim ADJMIN(1 To 2000) As Double"))
        assertEquals("MI<Key2> teamcount = new MI<>()",
            variable("Dim teamcount(1 To 30, 4000 To 9000) As Long"))
        assertEquals("MD<Key3> homesim = new MD<>()",
            variable("Dim homesim(1 To 1500, 1 To 31, 1 To 18) As Double"))
        val entity = variable("Dim rostersim As Recordset")
        assertEquals("Recordset rostersim",
            variable("Dim rostersim As Recordset"))
    }

    @Test
    fun equalsInIf() {
        assertEquals("If aaa!bbb == ccc Then ddd(xxx!yyy) = 1",
            equalsInIf("If aaa!bbb = ccc Then ddd(xxx!yyy) = 1"))
        assertEquals("If sched!AWAY == targetteam Or sched!HOME == targetteam Then redo(sched!GAMEID) = 1",
            equalsInIf("If sched!AWAY = targetteam Or sched!HOME = targetteam Then redo(sched!GAMEID) = 1"))
    }

    @Test
    fun forLoopBegin() {
        assertEquals("for (int i = 1; i <= count; i++) {",
            forLoopBegin("For i = 1 To count"))
        assertEquals("for (int j = 101; j <= systemcount + 101; j++) {",
            forLoopBegin("For j = 101 To systemcount + 101"))
    }

    @Test
    fun whileLoopBegin() {
        assertEquals("while (found == 0) {",
            whileLoopBegin("While found = 0"))
    }

    @Test
    fun forLoopEnd() {
        assertEquals("}", forLoopEnd("Next i"))
    }

    @Test
    fun putMultiple2() {
        val fields = setOf("awayid")
        assertEquals("awayid.put(new Key2(rostersim.GAMEID, awaycount(rostersim.GAMEID)), rostersim.PLAYERID)",
            multiple2("awayid(rostersim.GAMEID, awaycount(rostersim.GAMEID)) = rostersim.PLAYERID", fields))
    }

    @Test
    fun putMultiple3() {
        val fields = setOf("awaysim")
        assertEquals("awaysim.put(new Key3(rostersim.GAMEID, awaycount(rostersim.GAMEID), 1), rostersim.POS)",
            multiple3("awaysim(rostersim.GAMEID, awaycount(rostersim.GAMEID), 1) = rostersim.POS", fields))
        assertEquals("if (rostersim.POS == 1) awaysim.put(new Key3(rostersim.GAMEID, awaycount(rostersim.GAMEID), 6), rostersim.FGP2 + (deffgadj(1, HID(rostersim.GAMEID)) * 0.1 + 0.8 * deffgadj(4, HID(rostersim.GAMEID))))",
            multiple3("if (rostersim.POS == 1) awaysim(rostersim.GAMEID, awaycount(rostersim.GAMEID), 6) = rostersim.FGP2 + (deffgadj(1, HID(rostersim.GAMEID)) * 0.1 + 0.8 * deffgadj(4, HID(rostersim.GAMEID)))", fields))
        assertEquals("awaysim.put(new Key3(rostersim.GAMEID, awaycount.get(rostersim.GAMEID), 6), awaysim.get(new Key3(rostersim.GAMEID, awaycount.get(rostersim.GAMEID), 6)) * adjawayx.get(rostersim.GAMEID) + fgpercadj.get(rostersim.TEAMID))",
            multiple3("awaysim.put(new Key3(rostersim.GAMEID, awaycount.get(rostersim.GAMEID), 6), awaysim(rostersim.GAMEID, awaycount.get(rostersim.GAMEID), 6) * adjawayx.get(rostersim.GAMEID) + fgpercadj.get(rostersim.TEAMID))", fields))
    }

    @Test
    fun putMapSafe() {
        val fields = setOf("ADJMIN", "hmin", "awaymov", "ADJTO", "ADJFGA", "ADJFTA", "ADJAST")
        assertEquals("simgame_sim(polls, ADJMIN, hmin)",
            mapSafe("simgame_sim(polls, ADJMIN(), hmin())", fields))
        assertEquals("ADJMIN.put(ROSTER.PLAYERID, ROSTER.ADJMIN)",
            mapSafe("ADJMIN(ROSTER.PLAYERID) = ROSTER.ADJMIN", fields))
        assertEquals("hmin.put(BOXSCORES.TEAMID, hmin.get(BOXSCORES.TEAMID) + BOXSCORES.MIN)",
            mapSafe("hmin(BOXSCORES.TEAMID) = hmin(BOXSCORES.TEAMID) + BOXSCORES.MIN", fields))
        assertEquals("if (awaymov.get(i) < 0) awaymov.put(i, 0)",
            mapSafe("if (awaymov(i) < 0) awaymov(i) = 0", fields))
        assertEquals("rostersim.To = (rostersim.To * ADJTO.get(rostersim.PLAYERID)) / ((ADJFGA.get(rostersim.PLAYERID) + ADJFTA.get(rostersim.PLAYERID) + ADJTO.get(rostersim.PLAYERID) + ADJAST.get(rostersim.PLAYERID)) / 4)",
            mapSafe("rostersim.To = (rostersim.To * ADJTO(rostersim.PLAYERID)) / ((ADJFGA(rostersim.PLAYERID) + ADJFTA(rostersim.PLAYERID) + ADJTO(rostersim.PLAYERID) + ADJAST(rostersim.PLAYERID)) / 4)", fields))
    }

    @Test
    fun putEntity() {
        val entities = setOf("rostersim")
        assertEquals("rostersim.setFGP2(rostersim.FGP2 * ADJFG2.get(rostersim.PLAYERID))",
            putEntity("rostersim.FGP2 = rostersim.FGP2 * ADJFG2.get(rostersim.PLAYERID)", entities))
        assertEquals("rostersim.setFGA((rostersim.FGA * ADJFGA.get(rostersim.PLAYERID)) / ((ADJFGA.get(rostersim.PLAYERID) + ADJFTA.get(rostersim.PLAYERID) + ADJTO.get(rostersim.PLAYERID) + ADJAST.get(rostersim.PLAYERID)) / 4))",
            putEntity("rostersim.FGA = (rostersim.FGA * ADJFGA.get(rostersim.PLAYERID)) / ((ADJFGA.get(rostersim.PLAYERID) + ADJFTA.get(rostersim.PLAYERID) + ADJTO.get(rostersim.PLAYERID) + ADJAST.get(rostersim.PLAYERID)) / 4)", entities))
    }
    @Test
    fun getEntity() {
        val entities = setOf("TEAMID", "sched", "advisor", "rostersim")
        assertEquals("movcoord.put(TEAMID.getID(), TEAMID.getXCOORD())",
            getEntities("movcoord.put(TEAMID.ID, TEAMID.XCOORD)", entities))
        assertEquals("""advisor.setdlr1_trend(advisor.getDLR1_TREND() & "   " & sched.getINJAWAY());""",
            getEntities("""advisor.setdlr1_trend(advisor.dlr1_trend & "   " & sched.INJAWAY);""", entities))
        assertEquals("rostersim.setFGA((rostersim.getFGA() * ADJFGA.get(rostersim.getPLAYERID())) / ((ADJFGA.get(rostersim.getPLAYERID()) + ADJFTA.get(rostersim.getPLAYERID()) + ADJTO.get(rostersim.getPLAYERID()) + ADJAST.get(rostersim.getPLAYERID())) / 4))",
            getEntities("rostersim.setFGA((rostersim.FGA * ADJFGA.get(rostersim.PLAYERID)) / ((ADJFGA.get(rostersim.PLAYERID) + ADJFTA.get(rostersim.PLAYERID) + ADJTO.get(rostersim.PLAYERID) + ADJAST.get(rostersim.PLAYERID)) / 4))", entities))
    }

    @Test
    fun saveEntity() {
        val entities = setOf("rostersim")
        assertEquals("Ebean.save(rostersim)",
            saveEntity("rostersim.UPDATE", entities))
    }

    @Test
    fun comment() {
        assertEquals("//rostersim.Close",
            comment("rostersim.Close"))
        assertEquals("//Recordset rostersim",
            comment("Recordset rostersim"))
        assertNull(
            comment("MapSafeI ROOKIE = new MapSafeI()"))
    }

    @Test
    fun semicolon() {
        assertEquals("MapSafeI ROOKIE = new MapSafeI();",
            semicolon("MapSafeI ROOKIE = new MapSafeI()"))
        assertNull(
            semicolon("}"))
    }

}
