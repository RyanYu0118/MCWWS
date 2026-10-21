import json,math,random
from pathlib import Path
folder=Path('tools/mcwws-build-bridge-mcp/island-magic-links-20261020')
survey=json.loads((folder/'island-survey.json').read_text())
castle=json.loads(Path('tools/mcwws-build-bridge-mcp/island-castle-pointed-towers-20261020/plan.json').read_text())
cols={(c['x'],c['z']):c for c in survey['columns']}
known={(p['x'],p['y'],p['z']) for p in castle['blocks']}
def free(x,y,z):return (x,y,z)not in known and y<cols.get((x,z),{'min':320})['min']
streams=[];blocks={};seed=random.Random(20261020)
for index,tower in enumerate(castle['towers']):
 x0,z0=-346+tower['x'],2778+tower['z'];y0=tower['peak']+1
 candidates=[c for c in survey['columns'] if (c['x']-x0)**2+(c['z']-z0)**2<=100 and 185<=c['min']<=224 and c['count']>=8]
 candidates.sort(key=lambda c:(c['x']-x0)**2+(c['z']-z0)**2+.05*(c['min']-210)**2)
 best=None
 for anchor in candidates[:30]:
  for radius in [5.5,5,4.5,4,3.5]:
   for turns in [1.75,1.5,1.25]:
    for phase_i in range(12):
     phase=phase_i*math.pi/6;end=anchor['min']-1;steps=math.ceil((end-y0)*2)
     points=[]
     for i in range(steps+1):
      u=i/steps;amp=radius*math.sin(math.pi*u)**.65;a=phase+turns*2*math.pi*u
      x=round(x0+(anchor['x']-x0)*u+amp*math.cos(a))
      z=round(z0+(anchor['z']-z0)*u+amp*math.sin(a))
      y=round(y0+(end-y0)*u)
      if not free(x,y,z):break
      points.append((x,y,z))
     if len(points)==steps+1:
      best=(anchor,radius,turns,phase,points);break
    if best:break
   if best:break
  if best:break
 if not best:raise RuntimeError(f'No clean spiral from tower {index}')
 anchor,radius,turns,phase,points=best
 emitted=set()
 for i,(x,y,z)in enumerate(points):
  u=i/(len(points)-1)
  # Fragmented ribbons, with dense crystal nodes and a lighter satellite dust field.
  if i%9<6 or i<4 or i>len(points)-5:
   q=seed.random();color=('cyan' if index%2==0 else 'purple') if q<.68 else ('light_blue' if index%2==0 else 'magenta')
   material='sea_lantern' if i%19==0 else 'amethyst_block' if q>.87 else color+'_stained_glass'
   blocks[(x,y,z)]='minecraft:'+material;emitted.add((x,y,z))
   if i%19==0:
    for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
     if free(x+dx,y,z+dz):blocks.setdefault((x+dx,y,z+dz),'minecraft:light_blue_stained_glass');emitted.add((x+dx,y,z+dz))
  if i%3==0 and .04<u<.96:
   a=seed.random()*2*math.pi;r=seed.uniform(1.5,3.5)
   dx,dz=round(math.cos(a)*r),round(math.sin(a)*r);dy=seed.choice([-1,0,1])
   if y+dy>tower['peak'] and free(x+dx,y+dy,z+dz):
    blocks.setdefault((x+dx,y+dy,z+dz),'minecraft:'+seed.choice(['light_blue_stained_glass','purple_stained_glass','amethyst_block']))
    emitted.add((x+dx,y+dy,z+dz))
 # Explicit contact points at the spire and immediately under the selected island block.
 blocks[(x0,y0,z0)]='minecraft:pearlescent_froglight'
 blocks[(anchor['x'],anchor['min']-1,anchor['z'])]='minecraft:pearlescent_froglight'
 streams.append({'tower':[x0,tower['peak'],z0],'anchor':[anchor['x'],anchor['min'],anchor['z']],'anchorBlock':anchor['bottom'],'radius':radius,'turns':turns,'phase':phase,'centerline':points,'particles':len(emitted)})
placements=[{'x':x,'y':y,'z':z,'block':b} for (x,y,z),b in blocks.items()]
bounds={a:[min(p[a] for p in placements),max(p[a] for p in placements)] for a in ['x','y','z']}
plan={'world':'world','bounds':bounds,'streams':streams,'blocks':placements,'materials':{b:sum(p['block']==b for p in placements) for b in set(blocks.values())}}
(folder/'plan.json').write_text(json.dumps(plan))
print(json.dumps({'bounds':bounds,'blocks':len(placements),'streams':[{k:v for k,v in s.items() if k!='centerline'} for s in streams],'materials':plan['materials']},indent=2))
