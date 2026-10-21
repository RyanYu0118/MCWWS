import json,math,random,collections
from pathlib import Path
root=Path('tools/mcwws-build-bridge-mcp')
old=json.loads((root/'island-magic-links-20261020/plan.json').read_text())
survey=json.loads((root/'island-magic-links-20261020/island-survey.json').read_text())
castle=json.loads((root/'island-castle-pointed-towers-20261020/plan.json').read_text())
folder=root/'island-magic-splash-20261020';folder.mkdir(exist_ok=True)
cols={(p['x'],p['z']):p for p in survey['columns']}
occupied={(p['x'],p['y'],p['z']) for p in castle['blocks']}
previous={(p['x'],p['y'],p['z']):p['block'] for p in old['blocks']}
blocks=dict(previous);rng=random.Random(202610204)
groups=collections.Counter();perStream=[]
def put(x,y,z,b,group,minY):
 if y<minY or y>221 or (x,y,z)in occupied:return
 if y>=cols.get((x,z),{'min':320})['min']-1:return
 if (x,y,z)in blocks:return
 blocks[(x,y,z)]='minecraft:'+b;groups[group]+=1
for streamIndex,s in enumerate(old['streams']):
 countBefore=len(blocks);points=s['centerline'];minimum=s['tower'][1]+1
 colors=['cyan_stained_glass','light_blue_stained_glass','purple_stained_glass'] if streamIndex%2==0 else ['purple_stained_glass','magenta_stained_glass','light_blue_stained_glass']
 for i,(x,y,z)in enumerate(points):
  u=i/(len(points)-1)
  # Build an irregular, mostly transparent core about three to five blocks across.
  if i%2==0 and i%13<11:
   rad=(1.15+1.05*math.sin(math.pi*u))
   for j in range(4):
    angle=rng.random()*2*math.pi;r=rad*math.sqrt(rng.random())
    dx,dz=round(math.cos(angle)*r),round(math.sin(angle)*r)
    b=rng.choice(colors+colors+['amethyst_block'])
    put(x+dx,y+rng.choice([-1,0,0,1]),z+dz,b,'thickened_core',minimum)
  # Bright crystal clusters appear intermittently within the expanded energy body.
  if i%21==5 and .04<u<.96:
   dx,dz=rng.choice([(2,0),(-2,0),(0,2),(0,-2)])
   put(x+dx,y,z+dz,'sea_lantern','glowing_cluster',minimum)
   for a,b in [(1,0),(-1,0),(0,1)]:put(x+dx+a,y,z+dz+b,rng.choice(colors),'glowing_cluster',minimum)
  # Fine outer dust has a broad radius and a falling density toward its outer edge.
  if i%2==0 and .025<u<.975:
   for j in range(4):
    a=rng.random()*2*math.pi;r=rng.triangular(3.2,11.5,5.5)
    dx,dz=round(math.cos(a)*r),round(math.sin(a)*r)
    b=rng.choice(colors+colors+['amethyst_block'])
    if rng.random()<.045:b='pearlescent_froglight'
    put(x+dx,y+rng.randint(-3,3),z+dz,b,'outer_dust',minimum)
  # Broken, outward-curving spurts show the direction of the spray rather than a uniform cloud.
  if i%11==4 and .07<u<.93:
   a=s['phase']+s['turns']*2*math.pi*u+rng.uniform(-1.1,1.1)
   for j in range(rng.randint(4,7)):
    distance=2.8+j*1.4
    if j%4==3:continue
    dx=round(distance*math.cos(a)+rng.uniform(-.6,.6))
    dz=round(distance*math.sin(a)+rng.uniform(-.6,.6))
    dy=round(j*.45-j*j*.07)
    put(x+dx,y+dy,z+dz,rng.choice(colors+['amethyst_block']),'spray_trails',minimum)
 perStream.append({'tower':s['tower'],'added':len(blocks)-countBefore})
placements=[{'x':x,'y':y,'z':z,'block':b} for (x,y,z),b in blocks.items()]
patch=[p for p in placements if (p['x'],p['y'],p['z'])not in previous]
bounds={a:[min(p[a] for p in placements),max(p[a] for p in placements)] for a in ['x','y','z']}
plan={**old,'bounds':bounds,'blocks':placements,'patch':patch,'previousBlocks':old['blocks'],'groups':dict(groups),'perStream':perStream}
(folder/'plan.json').write_text(json.dumps(plan))
print(json.dumps({'bounds':bounds,'total':len(placements),'added':len(patch),'groups':dict(groups),'perStream':perStream},indent=2))
