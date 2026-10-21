import json,math,collections
from pathlib import Path
folder=Path('tools/mcwws-build-bridge-mcp/island-castle-expansion-20261020')
p=json.loads((folder/'plan.json').read_text())
b={(v['x'],v['y'],v['z']):v['block'] for v in p['blocks']}
fails=[]
for s in p['stairSteps']:
 for h in [1,2]:
  pos=(s['x'],s['y']+h,s['z'])
  if pos in b:fails.append(('stair headroom',pos,b[pos]))
# All three decks must be continuous and connected to the four stairs.
connectivity=[]
for f in [102,110,118]:
 walk={(x,z) for (x,y,z),v in b.items() if y==f and not any(a in v for a in ['wall','bars','lantern','ladder','slab']) and (x,f+1,z)not in b and (x,f+2,z)not in b}
 start=(p['landings'][[102,110,118].index(f)]['x'],p['landings'][[102,110,118].index(f)]['z'])
 seen={start};q=collections.deque([start])
 while q:
  x,z=q.popleft()
  for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
   t=x+dx,z+dz
   if t in walk and t not in seen:seen.add(t);q.append(t)
 anchors=[(v['x'],v['z']) for v in p['landings'] if v['y']==f+1]
 missing=[v for v in anchors if v not in seen]
 angles=[]
 for i in range(72):
  a=i*math.pi/36
  samples=[(-346+round(r*math.cos(a)),2778+round(r*math.sin(a))) for r in [34,35,36,37,38]]
  if not any(v in seen for v in samples):angles.append(i*5)
 connectivity.append({'floor':f,'reachable':len(seen),'missingLandings':missing,'unreachableAngles':angles})
 if missing or angles:fails.append(('deck disconnected',connectivity[-1]))
# Lanterns have a real supporting floor or ceiling.
for (x,y,z),v in b.items():
 if 'lantern' in v:
  pos=(x,y+1 if 'hanging=true' in v else y-1,z)
  if pos not in b:fails.append(('unsupported lantern',(x,y,z)))
# Cardinal five-air gallery cross section, away from doors and tower intersections.
for y in [96,104,112]:
 for dz in range(-38,-33):
  if (-346,y,2778+dz)in b:fails.append(('gallery blocked',(-346,y,2778+dz)))
report={'connectivity':connectivity,'failures':fails}
(folder/'geometry-check.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
raise SystemExit(bool(fails))
