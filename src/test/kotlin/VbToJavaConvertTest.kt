import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

internal class VbToJavaConvertTest {

    @Test
    fun convert() {
        val lines =
            """
private double startertipadj(int playercount, MapSafeD otip, MapSafeI odepth, int startingtiptotal) {
    usesimadj = 1 '
Dim PLAYERID As Integer
Dim rostersim As Recordset
Dim advisor As Recordset
Dim sched As Recordset
Dim ADJMIN(1 To 2000) As Double
Dim ADJFGA(1 To 2000) As Double
Dim ADJFTA(1 To 2000) As Double
Dim ADJTO(1 To 2000) As Double
Dim ADJAST(1 To 2000) As Double
Dim redo(1 To 2000) As Integer
Dim awaysim(1 To 1500, 1 To 31, 1 To 18) As Double
Dim awaycount(1 To 1500) As Integer
Dim deffgadj(1 To 5, 1 To 31) As Double '1 = PG, 2 = SG/SF, 3 = PF/C, 4 = 2PT SHOTS, 5 = 3 PT SHOTS
Dim HID(1 To 1500) As Integer
Dim schedfgadj(1 To 1500, 1 To 2) As Double
Dim awaymov(1 To 1500) As Double
Dim startertip As Double
Dim benchtip As Double

For i = 1 To count
    If sched!AWAY = targetteam Or sched!HOME = targetteam Then redo(sched!GAMEID) = 1
    sched.MoveNext
Next i
    ADJMIN(ROSTER!PLAYERID) = ROSTER!ADJMIN
                rostersim!FGA = (rostersim!FGA * ADJFGA(rostersim!PLAYERID)) / ((ADJFGA(rostersim!PLAYERID) + ADJFTA(rostersim!PLAYERID) + ADJTO(rostersim!PLAYERID) + ADJAST(rostersim!PLAYERID)) / 4)
                rostersim.UPDATE
            awaysim(rostersim!GAMEID, awaycount(rostersim!GAMEID), 1) = rostersim!POS
            If rostersim!POS = 1 Then awaysim(rostersim!GAMEID, awaycount(rostersim!GAMEID), 6) = rostersim!FGP2 + (deffgadj(1, HID(rostersim!GAMEID)) * 0.1 + 0.8 * deffgadj(4, HID(rostersim!GAMEID)))
            awaysim(rostersim!GAMEID, awaycount(rostersim!GAMEID), 6) = awaysim(rostersim!GAMEID, awaycount(rostersim!GAMEID), 6) + schedfgadj(rostersim!GAMEID, 1) '- 0.001
                If awaymov(i) < 0 Then awaymov(i) = 0
    If odepth(i) = 1 Then startertip = startertip + otip(i)
    If odepth(i) = 2 Then benchtip = benchtip + otip(i)
    If statid = 21 Then 'PTS + ASSIST
            advisor!dlr1_trend = advisor!dlr1_trend & "   " & sched!INJAWAY
"""
        val expectedLines =
            """
private double startertipadj(int playercount, MapSafeD otip, MapSafeI odepth, int startingtiptotal) {
    usesimadj = 1;
int PLAYERID = 0;
//Recordset rostersim
//Recordset advisor
//Recordset sched
MapSafeD ADJMIN = new MapSafeD();
MapSafeD ADJFGA = new MapSafeD();
MapSafeD ADJFTA = new MapSafeD();
MapSafeD ADJTO = new MapSafeD();
MapSafeD ADJAST = new MapSafeD();
MapSafeI redo = new MapSafeI();
MD<Key3> awaysim = new MD<>();
MapSafeI awaycount = new MapSafeI();
MD<Key2> deffgadj = new MD<>(); //1 = PG, 2 = SG/SF, 3 = PF/C, 4 = 2PT SHOTS, 5 = 3 PT SHOTS
MapSafeI HID = new MapSafeI();
MD<Key2> schedfgadj = new MD<>();
MapSafeD awaymov = new MapSafeD();
double startertip = 0.0;
double benchtip = 0.0;

for (int i = 1; i <= count; i++) {
    if (sched.getAWAY() == targetteam || sched.getHOME() == targetteam) redo.put(sched.getGAMEID(), 1);
    //sched.MoveNext
}
    ADJMIN.put(ROSTER.PLAYERID, ROSTER.ADJMIN);
                rostersim.setFGA((rostersim.getFGA() * ADJFGA.get(rostersim.getPLAYERID())) / ((ADJFGA.get(rostersim.getPLAYERID()) + ADJFTA.get(rostersim.getPLAYERID()) + ADJTO.get(rostersim.getPLAYERID()) + ADJAST.get(rostersim.getPLAYERID())) / 4));
                Ebean.save(rostersim);
            awaysim.put(new Key3(rostersim.getGAMEID(), awaycount.get(rostersim.getGAMEID()), 1), rostersim.getPOS());
            if (rostersim.getPOS() == 1) awaysim.put(new Key3(rostersim.getGAMEID(), awaycount.get(rostersim.getGAMEID()), 6), rostersim.getFGP2() + (deffgadj.get(new Key2(1, HID.get(rostersim.getGAMEID()))) * 0.1 + 0.8 * deffgadj.get(new Key2(4, HID.get(rostersim.getGAMEID())))));
            awaysim.put(new Key3(rostersim.getGAMEID(), awaycount.get(rostersim.getGAMEID()), 6), awaysim.get(new Key3(rostersim.getGAMEID(), awaycount.get(rostersim.getGAMEID()), 6)) + schedfgadj.get(new Key2(rostersim.getGAMEID(), 1))); //- 0.001
                if (awaymov.get(i) < 0) awaymov.put(i, 0);
    if (odepth.get(i) == 1) startertip = startertip + otip.get(i);
    if (odepth.get(i) == 2) benchtip = benchtip + otip.get(i);
    if (statid == 21) { //PTS + ASSIST
            advisor.setDLR1_TREND(advisor.getDLR1_TREND() + "   " + sched.getINJAWAY());
"""

        assertLinesMatch(expectedLines.lines(), convert(lines.lines()))
    }
}