package marlin.music;

import marlin.I;
import marlin.Reaction.*;
import marlin.UC;
import marlin.graphicsLib.G;
import marlin.graphicsLib.Window;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;

public class Music1 extends Window {

    static {
        new Layer("BACK");
        new Layer("NOTE");
        new Layer("FORE");
    }

    public static Music.Page PAGE = new Music.Page();
    public static Music.Sys.Fmt SYSFMT = null;
    public static ArrayList<Music.Sys> SYSTEMS = new ArrayList<>();

    public Music1() {
        super("Music1", UC.screenWidth, UC.screenHeight);
        Reaction.initialAction = new I.Act(){
            public void act(Gesture g) { SYSFMT = null; }
        };
        Reaction.initialReactions.addReaction(new Reaction("E-E"){
            public int bid(Gesture g){
                if(SYSFMT == null){ return 0; }
                int y = g.vs.yMid();
                if (y > PAGE.top + SYSFMT.height() + 15){ return 100; }
                    else{ return UC.noBid; }
            }
            public void act(Gesture g){
                int y = g.vs.yMid();
                if (SYSFMT == null){
                    PAGE.top = y;
                    SYSFMT = new Music.Sys.Fmt();
                    SYSTEMS.clear();
                    new Music.Sys();
                }
                SYSFMT.addNewStaff(y);
            }
        });
        Reaction.initialReactions.addReaction(new Reaction("E-W") {
            @Override
            public int bid(Gesture g) {
                if (SYSFMT ==null){ return UC.noBid; }
                int y = g.vs.yMid();
                if (y > SYSTEMS.get(SYSTEMS.size()-1).yBot() + 15){ return  100; }
                return UC.noBid;
            }

            @Override
            public void act(Gesture g) {
                int y = g.vs.yMid();
                if(SYSTEMS.size() == 1){
                    PAGE.sysGap = y - (PAGE.top + SYSFMT.height());
                }
                new Music.Sys();
            }
        });
    }

    public void paintComponent(Graphics g){
        G.fillBackground(g,Color.WHITE);
        g.setColor(Color.BLACK);
        Ink.BUFFER.show(g);
        Layer.ALL.show(g);
//        int H = 8;
//        Glyph.CLEF_G.showAt(g, H, 100, PAGE.top + 4*H);
//        Glyph.HEAD_Q.showAt(g, H, 180, PAGE.top + 4*H);
//        g.drawRect(180, PAGE.top + 3*H, 24*H/10, 2*H);
    }

    public void mousePressed(MouseEvent me){ Gesture.AREA.dn(me.getX(),me.getY());repaint(); }
    public void mouseDragged(MouseEvent me){ Gesture.AREA.drag(me.getX(),me.getY());repaint(); }
    public void mouseReleased(MouseEvent me){ Gesture.AREA.up(me.getX(),me.getY());repaint(); }


    //-----------Music-----------
    public static class Music{
        public static class Sys extends Mass {
            public ArrayList<Staff> staffs = new ArrayList<>();

            Time.List times;

            public int ndx;

            public Sys() {
                super("BACK");
                ndx = SYSTEMS.size();
                SYSTEMS.add(this);
                makeStaffsMatchSysFmt();
                times = new Time.List(this);
            }

            public Time getTime(int x){ return times.getTime(x); }

            public int yTop(){
                return PAGE.top + ndx*(SYSFMT.height() + PAGE.sysGap);
            }

            public int yBot(){
                return yTop() + SYSFMT.height();
            }

            public void show(Graphics g) {
                SYSFMT.showAt(g,yTop());
                g.drawLine(PAGE.left,yTop(),PAGE.left,yBot());
            }

            public void makeStaffsMatchSysFmt(){
                while(staffs.size() < SYSFMT.size()){
                    new Staff(this);
                }
            }

            public static class Fmt extends ArrayList<Staff.Fmt> {
                public int MAXH = UC.defaultStaffLineSpace;
                public int height(){
                 Staff.Fmt last = get(size()-1);
                 return last.dy + last.height();
                }

                public void addNewStaff(int y){
                    new Staff.Fmt(y - PAGE.top);
                    for(Sys s: SYSTEMS){
                        s.makeStaffsMatchSysFmt();
                    }
                }

                public void showAt(Graphics g, int y){
                    for(Staff.Fmt sf: this){
                        sf.showAt(g,y+sf.dy);
                    }
                }

            }
        }

        public static class Staff extends Mass{
            public Sys sys;
            public int ndx;
            public int H(){return SYSFMT.get(ndx).H;}

            public Staff(Sys sys) {
                super("BACK");
                this.sys = sys;
                this.ndx = sys.staffs.size();
                sys.staffs.add(this);
                addReaction(new Reaction("S-S") {//creates a bar
                    public int bid(Gesture g) {
                        int x= g.vs.xMid(), y1 = g.vs.yLow(), y2 = g.vs.yHi();
                        if (x<PAGE.left || x > PAGE.right + UC.barToMarginSnap){ return UC.noBid; }
                        int dt = Math.abs(y1 - Staff.this.yTop());
                        int db = Math.abs(y2 - Staff.this.yBot());
                        if (dt > 15 || db > 15){ return UC.noBid; }
                        return dt + db + 20;
                    }

                    public void act(Gesture g) { new Bar(Staff.this.sys, g.vs.xMid()); }
                });

                addReaction(new Reaction("S-S") { // toggle barContinues
                    public int bid(Gesture g) {
                        if (Staff.this.sys.ndx != 0) {return UC.noBid;}
                        int y1 = g.vs.yLow(), y2 = g.vs.yHi();
                        if (Staff.this.ndx == SYSFMT.size() - 1){ return UC.noBid; }
                        if (Math.abs(y1-Staff.this.yBot()) > 20){return UC.noBid;}
                        Staff nextStaff = Staff.this.sys.staffs.get(Staff.this.ndx+1);
                        if (Math.abs(y2 - nextStaff.yTop()) > 20){return UC.noBid;}
                        return 10;
                    }
                    public void act(Gesture g) {
                        SYSFMT.get(Staff.this.ndx).toggleBarContinues();
                    }
                });

                addReaction(new Reaction("SW-SW") {
                    public int bid(Gesture g) {
                        int x = g.vs.xMid();
                        int y = g.vs.yMid();
                        if(x < PAGE.left || x > PAGE.right){return UC.noBid;}
                        int top = Staff.this.yTop();
                        int bot = Staff.this.yBot();
                        if (y < top || y > bot){return UC.noBid;}
                        return 20;
                    }
                    public void act(Gesture g) {
                        new Head(Staff.this, g.vs.xMid(), g.vs.yMid());
                    }
                });

                addReaction(new Reaction("E-S") {
                    public int bid(Gesture g) {
                        int x = g.vs.xMid();
                        int y = g.vs.yMid();
                        if(x < PAGE.left || x > PAGE.right){return UC.noBid;}
                        int top = Staff.this.yTop();
                        int bot = Staff.this.yBot();
                        if (y < top || y > bot){return UC.noBid;}
                        return 20;
                    }

                    public void act(Gesture g) {
                        (new Rest(Staff.this, Staff.this.sys.getTime(g.vs.xMid()))).nFlags++;
                    }
                });
            }

            public void show(Graphics g) { }
            public int yTop(){ return sys.yTop() + SYSFMT.get(ndx).dy; }
            public int yBot(){ return yTop() + SYSFMT.get(ndx).height(); }
            public int yLine(int line){return yTop() + line*H();}


            public static class Fmt {
                public int nLines = 5;
                public int H = UC.defaultStaffLineSpace;
                public int dy = 0;
                public boolean barContinues = false;

                public void toggleBarContinues(){
                    barContinues = !barContinues;
                }

                public Fmt(int dy){ this.dy = dy; SYSFMT.add(this);}

                public int height(){ return (nLines - 1) * 2 * H; }

                public void showAt(Graphics g, int y){
                    for(int i =0;i < nLines; i++){
                        int yy = y + 2* i * H;
                        g.drawLine(PAGE.left,yy,PAGE.right,yy);
                    }
                }
            }
        }

        public static class Bar extends Mass{
            public Sys sys;
            public int x;
            public static int barType;
            public static int LEFT = 4, RIGHT = 8;

            public Bar(Sys sys, int x) {
                super("BACK");
                this.sys = sys;
                this.x = x;
                if (Math.abs(x - PAGE.right) < UC.barToMarginSnap){ this.x = PAGE.right; }
                this.barType = 0;
                addReaction(new Reaction("S-S") {//cycling the barType
                    public int bid(Gesture g) {
                        int x = g.vs.xMid(), y1 = g.vs.yLow(), y2 = g.vs.yHi();
                        if (Math.abs(x - Bar.this.x) > UC.barToMarginSnap){ return UC.noBid; }
                        if (y1 < Bar.this.sys.yTop() - 20){ return UC.noBid; }
                        if (y2 > Bar.this.sys.yBot() + 20){ return UC.noBid; }
                        return Math.abs(x - Bar.this.x);
                    }
                    public void act(Gesture g) {
                        Bar.this.cycleType();
                    }
                });

                addReaction(new Reaction("DOT"){ // Dot this Bar
                    public int bid(Gesture g){
                        int x = g.vs.xMid(); int y = g.vs.yMid();
                        if(y < Bar.this.sys.yTop() || y > Bar.this.sys.yBot()){return UC.noBid;}
                        int dist = Math.abs(x - Bar.this.x);
                        if(dist > 3*SYSFMT.MAXH){return UC.noBid;}
                        return dist;
                    }
                    public void act(Gesture g){
                        if(g.vs.xMid() < Bar.this.x){Bar.this.toggleLeft();} else {Bar.this.toggleRight();}
                    }
                });
            }

            public void cycleType(){
                barType++;
                if(barType > 2){ barType = 0; }
            }


            public void toggleLeft(){ barType = barType^LEFT;}
            public void toggleRight(){ barType = barType^RIGHT;}




            public void show(Graphics g) {
                int yTop = sys.yTop(), y1 = 0, y2 = 0;
                boolean justSawBreak = true;
                for (Staff.Fmt sf: SYSFMT){
                    int top = yTop + sf.dy;
                    int bot = top + sf.height();
                    if (justSawBreak){ y1 = top;}
                    justSawBreak = !sf.barContinues;
                    if (justSawBreak){drawLines(g,x,y1,bot);}
                    if (barType > 3){ drawDots(g,x,top); }
                }
            }

            public static void wings(Graphics g, int x, int y1, int y2, int dx, int dy){
                g.drawLine(x,y1,x+dx,y1-dy);
                g.drawLine(x,y2,x+dx,y2+dy);
            }

            public static void fatBar(Graphics g, int x, int y1, int y2, int dx){
                g.fillRect(x,y1,dx,y2-y1);
            }

            public static void thinBar(Graphics g, int x, int y1, int y2){
                g.drawLine(x,y1,x,y2);
            }



            public void drawLines(Graphics g, int x, int y1, int y2){
                int H = SYSFMT.MAXH;
                if(barType == 0){ thinBar(g, x, y1, y2);}
                if(barType == 1){ thinBar(g, x, y1, y2); thinBar(g, x-H, y1, y2);}
                if(barType == 2){ fatBar(g, x-H, y1, y2, H); thinBar(g, x-2*H, y1, y2);}
                if(barType >= 4){ fatBar(g, x-H, y1, y2, H); // all repeats have fat bar
                    if((barType&LEFT) != 0){thinBar(g, x-2*H, y1, y2); wings(g, x-2*H, y1, y2, -H, H);}
                    if((barType&RIGHT) != 0){thinBar(g, x+H, y1, y2); wings(g, x+H, y1, y2, H, H);}
                }
            }
            public void drawDots(Graphics g, int x, int top){ // from top of single staff
                // notice - this code ASSUMES nLine is 5. We will need to fix if we ever allow
                // not-standard staffs.
                int H = SYSFMT.MAXH;
                if((barType & LEFT) != 0){
                    g.fillOval(x-3*H, top+11*H/4, H/2, H/2);
                    g.fillOval(x-3*H, top+19*H/4, H/2, H/2);
                }
                if((barType & RIGHT) != 0){
                    g.fillOval(x+3*H/2, top+11*H/4, H/2, H/2);
                    g.fillOval(x+3*H/2, top+19*H/4, H/2, H/2);
                }
            }
        }

        public static class Page{
            public static int M = 50;
            public int top = M;
            public int left = M;
            public int bot = UC.screenHeight - M;
            public int right = UC.screenWidth - M;
            public int sysGap = 0;
        }

        public static class Time {
            public int x;
            public ArrayList<Head> heads = new ArrayList<>();

            private Time(int x, Sys sys){ this.x = x; sys.times.add(this); }

            public void stemHeads(Staff staff, boolean up, int y1, int y2){
                Stem s = new Stem(staff, up, this);
                for (Head h: heads){
                    int y = h.y();
                    if (y > y1 && y < y2){ h.joinStem(s); }
                }
                if (s.heads.size() == 0){
                    s.deleteMass();
                } else{
                    s.setWrongSide();
                }
            }

            public void unStemHeads(int y1, int y2){
                for (Head h: heads){
                    int y = h.y();
                    if (y > y1 && y < y2){ h.unStem(); }
                }
            }

            public static class List extends ArrayList<Time>{
                public Sys sys;
                public List(Sys sys){ this.sys = sys; }

                public Time getTime(int x){
                    if(size() == 0){ return new Time(x, sys); }
                    Time t = getClosestTime(x);
                    if(Math.abs(x - t.x) < UC.snapTime){
                        return t;
                    }
                    else{
                        return new Time(x, sys);
                    }
                }

                public Time getClosestTime(int x){
                    Time result = get(0);
                    int bestSoFar = Math.abs(x - result.x);
                    for (Time t: this){
                        int dist = Math.abs(x - t.x);
                        if(dist < bestSoFar){
                            bestSoFar = dist;
                            result = t;
                        }
                    }
                    return result;
                }
            }
        }

        public static abstract class Duration extends Mass{
            public Time time;
            public int nFlags = 0, nDot = 0;
            public Duration(Time time){
                super("NOTE");
                this.time = time;
            }
            public abstract void show(Graphics g);
            public void incFlag(){if(nFlags < 4){nFlags++;}}
            public void decFlag(){if(nFlags > -2){nFlags--;}}
            public void cycleDot(){nDot++; if(nDot > 3){nDot = 0;}}
        }

        public static class Rest extends Duration{
            public Staff staff;
            public int line = 4;
            public Rest(Staff staff, Time t){
                super(t);
                this.staff = staff;

                addReaction(new Reaction("E-E") {
                    public int bid(Gesture g) {
                        int y = g.vs.yMid();
                        int x1 = g.vs.xLow();
                        int x2 = g.vs.xHi();
                        int x = Rest.this.time.x;
                        if (x1 > x || x2 < x){return UC.noBid;}
                        return Math.abs(y - Rest.this.staff.yLine(4));
                    }

                    public void act(Gesture g) {
                        Rest.this.incFlag();
                    }
                });

                addReaction(new Reaction("W-W") {
                    public int bid(Gesture g) {
                        int y = g.vs.yMid();
                        int x1 = g.vs.xLow();
                        int x2 = g.vs.xHi();
                        int x = Rest.this.time.x;
                        if (x1 > x || x2 < x){return UC.noBid;}
                        return Math.abs(y - Rest.this.staff.yLine(4));
                    }

                    public void act(Gesture g) {
                        Rest.this.decFlag();
                    }
                });

                addReaction(new Reaction("DOT") {
                    public int bid(Gesture g) {
                        int y = g.vs.yMid();
                        int x = g.vs.xMid();
                        int yR = Rest.this.staff.yLine(4);
                        int xR = Rest.this.time.x;
                        if (x < xR + 2 || x > xR + 30 || y < yR - 30 || y > yR +30){return UC.noBid;}
                        return Math.abs(y - yR) + Math.abs(x - xR);
                    }

                    public void act(Gesture g) {
                        Rest.this.cycleDot();
                    }
                });
            }
            public void show(Graphics g) {
                int h = staff.H();
                int top = staff.yTop();
                int y = top + line*h;
                if(nFlags == -2){ Glyph.REST_W.showAt(g, h, time.x,y); }
                if(nFlags == -1){ Glyph.REST_H.showAt(g, h, time.x,y); }
                if(nFlags == 0){ Glyph.REST_Q.showAt(g, h, time.x,y); }
                if(nFlags == 1){ Glyph.REST_1F.showAt(g, h, time.x,y); }
                if(nFlags == 2){ Glyph.REST_2F.showAt(g, h, time.x,y); }
                if(nFlags == 3){ Glyph.REST_3F.showAt(g, h, time.x,y); }
                if(nFlags == 4){ Glyph.REST_4F.showAt(g, h, time.x,y); }
                for (int i = 0; i < nDot; i++){
                    g.fillOval(time.x + i*8 + 30, y-3*h/2, h/2, h/2);
                }
            }
        }

        public static class Head extends Mass implements Comparable<Head>{
            public Staff staff;
            public int line;
            public Time time;
            public Stem stem = null;
            public boolean wrongSide = false;
            public Glyph forcedGlyph = null;

            public Head(Staff staff, int x, int y){
                super("NOTE");
                this.staff = staff;
                this.time = staff.sys.getTime(x);
                time.heads.add(this);
                int h = staff.H();
                this.line = (y - staff.yTop() + h/2)/h;
                System.out.println("line = " + line);

                addReaction(new Reaction("S-S") {
                    public int bid(Gesture g) {
                        int x = g.vs.xMid(), y1 = g.vs.yLow(), y2 = g.vs.yHi();
                        int w = Head.this.W(), hY = Head.this.y();
                        if (y1 > y || y2 < y) {return UC.noBid;}
                        int hL = Head.this.time.x, hR = hL + W();
                        if (x < hL - w || x > hR + w) { return UC.noBid; }
                        if (x < hL + w/2) { return hL - x; }
                        if (x > hR - w/2) { return x - hR; }
                        return UC.noBid;
                    }

                    public void act(Gesture g) {
                        int x = g.vs.xMid(), y1 = g.vs.yLow(), y2 = g.vs.yHi();
                        Staff staff = Head.this.staff;
                        Time time = Head.this.time;
                        int w = Head.this.W();
                        boolean up = x > time.x + w/2;
                        if (Head.this.stem == null) {
                            time.stemHeads(staff, up, y1, y2);
                        } else{
                            time.unStemHeads(y1, y2);
                        }
                    }
                });

            }

            public void show(Graphics g) {
                int h = staff.H();
//                Glyph.HEAD_Q.showAt(g, h, time.x,line*h + staff.yTop());
                ((forcedGlyph != null)? forcedGlyph: normalGlyph()).showAt(g, h, x(), y());
                if (stem != null) {
                    int offSet = UC.restFirstDot;
                }
            }

            public void joinStem(Stem s){
                unStem();
                s.heads.add(this);
                stem = s;
            }

            public void unStem(){
                if (stem == null){return;}
                stem.heads.remove(this);
                if (stem.heads.size() == 0){
                    stem.deleteStem();
                }
                stem = null;
            }

            public int x(){
                if (stem == null || stem.heads.size() == 0|| !wrongSide){ return time.x; }
                return time.x + (stem.isUp? W(): -W());
            }

            public int y(){
                return staff.yLine(line);
            }

            public Glyph normalGlyph(){
                if(stem == null) {return Glyph.HEAD_Q;}
                if(stem.nFlags == -2) {return Glyph.HEAD_W;}
                if(stem.nFlags == -1) {return Glyph.HEAD_HALF;}
                return Glyph.HEAD_Q;
            }

            public int W(){ return (24*staff.H())/10; }

            public void deleteHead(){
                time.heads.remove(this);
                deleteMass();
            }

            public int compareTo(Head h) {
                return (staff.ndx != h.staff.ndx)? (staff.ndx - h.staff.ndx): (line - h.line);
            }
        }

        public static class Stem extends Duration{
            public ArrayList<Head> heads = new ArrayList<>();
            public boolean isUp = true;
            public Staff staff;

            public Stem(Staff staff, boolean up, Time time){
                super(time);
                this.staff = staff;
                isUp = up;
            }

            public void deleteStem(){
                deleteMass();
            }

            public void show(Graphics g) {
                if(nFlags > -2 && heads.size() > 0){
                    int x = x(), yH = yFirstHead(), yB = yBeamEnd();
                    g.drawLine(x, yH, x, yB);
                }

            }
            //helper
            public Head FirstHead(){
                return heads.get(isUp? heads.size()-1: 0);
            }
            //helper
            public Head lastHead(){
                return heads.get(isUp? 0: heads.size()-1);
            }

            public int yFirstHead(){
                Head h = FirstHead();
                return h.staff.yLine(h.line);
            }

            public int x(){
                Head h = FirstHead();
                return h.time.x + (isUp? h.W(): 0);
            }

            public int yBeamEnd(){
                Head h = lastHead();
                int line = h.line;
                line += isUp? -7: 7;
                int flagInc = (nFlags > 2)? 2*(nFlags-2): 0;
                line += isUp? -flagInc: flagInc;
                if((isUp && line > 4) || (!isUp && line < 4)){
                    line = 4;
                }
                return staff.yLine(line);
            }

            public void setWrongSide(){
                Collections.sort(heads);
                int i, last, next;
                if (isUp){
                    i = heads.size()-1;
                    last = 0;
                    next = -1;
                }else{
                    i = 0;
                    last = heads.size()-1;
                    next = 1;
                }
                Head pH = heads.get(i);
                pH.wrongSide = false;
                while (i != last){
                    i += next;
                    Head nH = heads.get(i);
                    nH.wrongSide = nH.staff == pH.staff && Math.abs(nH.line-pH.line) == 1 && !pH.wrongSide;
                    pH = nH;
                }
            }

        }
    }
}
