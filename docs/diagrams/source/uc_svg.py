import math
W,H=1400,1290
o=[]
o.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="DejaVu Sans, Arial" font-size="15">')
o.append('<defs><marker id="op" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="11" markerHeight="11" orient="auto"><path d="M1,1 L11,6 L1,11" fill="none" stroke="#333" stroke-width="1.6"/></marker></defs>')
o.append('<rect width="100%" height="100%" fill="white"/>')
BLUE="#2F5597"
o.append(f'<rect x="250" y="40" width="880" height="1220" rx="8" fill="none" stroke="{BLUE}" stroke-width="2.5"/>')
o.append(f'<text x="690" y="70" text-anchor="middle" font-weight="bold" font-size="18">CheckedIn System</text>')
RX,RY=128,30
UC={}
def uc(k,x,y,label):
    UC[k]=(x,y)
    lines=label.split('|')
    o.append(f'<ellipse cx="{x}" cy="{y}" rx="{RX}" ry="{RY}" fill="#F7F9FC" stroke="{BLUE}" stroke-width="2"/>')
    y0=y-(len(lines)-1)*9+5
    for i,l in enumerate(lines):
        o.append(f'<text x="{x}" y="{y0+i*18}" text-anchor="middle">{l}</text>')
def actor(x,y,name,stereo=None):
    o.append(f'<g stroke="{BLUE}" stroke-width="2.5" fill="#DCE6F2"><circle cx="{x}" cy="{y-38}" r="13"/><line x1="{x}" y1="{y-25}" x2="{x}" y2="{y+15}"/><line x1="{x-22}" y1="{y-12}" x2="{x+22}" y2="{y-12}"/><line x1="{x}" y1="{y+15}" x2="{x-18}" y2="{y+45}"/><line x1="{x}" y1="{y+15}" x2="{x+18}" y2="{y+45}"/></g>')
    o.append(f'<text x="{x}" y="{y+68}" text-anchor="middle" font-weight="bold">{name}</text>')
    if stereo: o.append(f'<text x="{x}" y="{y-62}" text-anchor="middle" font-style="italic" font-size="13">«{stereo}»</text>')
def edge_pt(k,tx,ty):
    x,y=UC[k]; a=math.atan2(ty-y,tx-x)
    # point on ellipse boundary toward target
    t=math.atan2(math.sin(a)/RY, math.cos(a)/RX)
    return x+RX*math.cos(t), y+RY*math.sin(t)
def assoc(ax,ay,k):
    px,py=UC[k][0]-RX,UC[k][1]
    o.append(f'<line x1="{ax}" y1="{ay}" x2="{px:.1f}" y2="{py:.1f}" stroke="#333" stroke-width="1.6"/>')
def dep(a,b,label,lx=None,ly=None):
    bx,by=UC[b]; ax,ay=UC[a]
    p1=edge_pt(a,bx,by); p2=edge_pt(b,ax,ay)
    o.append(f'<line x1="{p1[0]:.1f}" y1="{p1[1]:.1f}" x2="{p2[0]:.1f}" y2="{p2[1]:.1f}" stroke="#333" stroke-width="1.6" stroke-dasharray="7,5" marker-end="url(#op)"/>')
    mx=(p1[0]+p2[0])/2 if lx is None else lx; my=(p1[1]+p2[1])/2-6 if ly is None else ly
    for i,l in enumerate(label.split('|')):
        o.append(f'<text x="{mx:.0f}" y="{my+i*16:.0f}" text-anchor="middle" font-size="13" fill="#222" paint-order="stroke" stroke="white" stroke-width="5">{l}</text>')
A,B=480,900
uc('UC01',A,130,'UC-01 Sign In|(Email Code or GitHub)')
uc('UC02',B,215,'UC-02 Link|Student ID')
uc('UC03',A,215,'UC-03 Manage Sections|and Sessions')
uc('UC04',A,300,'UC-04 Build Room /|Lab Layout')
uc('UC05',A,385,'UC-05 Import|Class List')
uc('UC06',A,470,'UC-06 Assign Seats')
uc('UC07',B,470,'UC-07 Auto-Arrange|Alphabetically')
uc('UC08',A,570,'UC-08 Check Attendance')
uc('UC09',A,655,'UC-09 Edit Past|Attendance')
uc('UC10',B,655,'UC-10 Compute|Absence Hours')
uc('UC12',A,740,'UC-12 Review Excuse')
uc('UC16',B,840,'UC-16 Send Email|Notification')
uc('UC11',A,840,'UC-11 View At-Risk|Students')
uc('UC13',A,925,'UC-13 Export|Attendance')
uc('UC14',A,1070,'UC-14 View Own|Attendance')
uc('UC15',A,1170,'UC-15 Submit Excuse')
# actors
PX,PY=110,560; SX,SY=110,1080
for k in ['UC01','UC03','UC04','UC05','UC06','UC08','UC09','UC12','UC11','UC13']: assoc(PX+26,PY-10,k)
for k in ['UC01','UC14','UC15']: assoc(SX+26,SY-10,k)
EX=1280
def ext(k,y,name):
    x,yy=UC[k]; p=edge_pt(k,EX-30,y-10)
    o.append(f'<line x1="{p[0]:.1f}" y1="{p[1]:.1f}" x2="{EX-30}" y2="{y-10}" stroke="#333" stroke-width="1.6"/>')
actor(PX,PY,'Professor'); actor(SX,SY,'Student')
actor(EX,140,'GitHub Sign-In','external system'); actor(EX,600,'Brevo Email API','external system'); actor(EX,1100,'Cloudinary','external system')
o_ext=[]
ext('UC01',140,'')
ext('UC16',600,''); ext('UC01',600,'')
ext('UC15',1100,''); ext('UC12',1100,'')
dep('UC02','UC01','«extend»|(first sign-in)',lx=760,ly=150)
dep('UC07','UC06','«extend»',ly=462)
dep('UC08','UC10','«include»',lx=690,ly=598)
dep('UC09','UC10','«include»',lx=690,ly=650)
dep('UC12','UC10','«include»',lx=690,ly=716)
dep('UC12','UC16','«include»',lx=690,ly=810)
dep('UC16','UC10','«extend»|(threshold reached)',lx=828,ly=752)
o.append('</svg>')
open('usecase.svg','w').write('\n'.join(o))
