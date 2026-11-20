import json,collections,shutil,hashlib,time
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
P=Path('tools/mcwws-build-bridge-mcp/ocean-restoration-20261105')
meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');q=g.copy();D=np.load(P/'design.npz')
cut=D['cut'];t=D['target'];floor=D['floor'];roof=D['overhang'];protect=D['protect'];restore=D['restore'];dist=D['dist'];ground=D['ground']
H,W=cut.shape;zz,xx=np.mgrid[2800:2800+H,-1200:-1200+W]
def bid(n):
 n='minecraft:'+n
 if n not in ids:ids[n]=len(names);names.append(n)
 return ids[n]
air=bid('air');water=bid('water');stone=bid('stone');gravel=bid('gravel');sand=bid('sand');clay=bid('clay');ice=bid('ice');packed=bid('packed_ice');blue=bid('blue_ice');snow=bid('snow_block')
natural={'air','cave_air','void_air','water','bubble_column','lava','stone','andesite','diorite','granite','deepslate','tuff','calcite','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','sand','red_sand','sandstone','red_sandstone','gravel','clay','mud','packed_mud','blackstone','smooth_basalt','basalt','moss_block','moss_carpet','snow','snow_block','powder_snow','ice','packed_ice','blue_ice','dripstone_block','pointed_dripstone','sculk','sculk_vein','amethyst_block','budding_amethyst','magma_block','short_grass','tall_grass','fern','large_fern','seagrass','tall_seagrass','kelp','kelp_plant','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','brown_mushroom','red_mushroom','small_dripleaf','big_dripleaf','big_dripleaf_stem','azalea','flowering_azalea','dead_bush','sweet_berry_bush','pale_moss_block','pale_moss_carpet','leaf_litter'}
allowed=np.array([n.removeprefix('minecraft:') in natural or n.endswith(('_ore','_leaves')) for n in names])
patch=ndi.gaussian_filter(np.random.default_rng(6182).normal(size=(H,W)),12);patch/=patch.std()
def put(col,a,b,value):
 a=max(0,int(a));b=min(191,int(b))
 if a>b:return
 s=col[a:b+1];ok=allowed[s];s[ok]=value
for iz,ix in zip(*np.where(cut)):
 col=q[iz,:,ix];height=int(t[iz,ix]);base=int(floor[iz,ix]);
 if height>110 or (ground[iz,ix]>110 and not roof[iz,ix]):continue
 # Fill low pits and establish a sealed three-block substrate.
 if height>base:put(col,max(0,base+1),height-3,stone)
 put(col,height-2,height-1,stone if patch[iz,ix]<.75 else bid('andesite'))
 cap=gravel if patch[iz,ix]<.55 else sand if patch[iz,ix]<1.5 else clay
 if height>=63:cap=stone if height>75 else gravel
 put(col,height,height,cap)
 if height<62:put(col,height+1,62,water)
 # Recessed cliff roofs are immutable; trim only their lower shore where exposed.
 if not roof[iz,ix]:put(col,max(height+1,63),min(110,int(ground[iz,ix])+2),air)
 # Retain the existing ice rim and add broad, irregular coastal ice patches.
 if height<61 and ((g[iz,62,ix]==ice and dist[iz,ix]<15) or (xx[iz,ix]<-850 and 5<dist[iz,ix]<35 and patch[iz,ix]>.25)):
  put(col,62,62,ice)
# Raise only completely open low water columns, including water beneath the pier.
for iz,ix in zip(*np.where(restore)):
 for y in range(55,63):
  if q[iz,y,ix] in [air,ids.get('minecraft:cave_air',-1),water]:q[iz,y,ix]=water
# Sparse asymmetric icebergs. All their cells remain in the approved open excavation.
bergs=[]
for cx,cz,r,h,phase in [(-1010,3075,8,9,.8),(-955,3240,11,12,2.1),(-1030,3360,7,7,4.2)]:
 cells=0
 for z in range(cz-r-4,cz+r+5):
  for x in range(cx-r-4,cx+r+5):
   iz=z-2800;ix=x+1200
   if not cut[iz,ix] or protect[iz,ix] or dist[iz,ix]<20:continue
   dx=x-cx;dz=z-cz;theta=np.arctan2(dz,dx);rad=np.sqrt((dx/1.1)**2+(dz/.83)**2)
   edge=r*(1+.16*np.sin(theta*3+phase)+.1*np.cos(theta*5-phase))
   if rad>edge+2:continue
   if rad>edge:
    if patch[iz,ix]>-.5:q[iz,62,ix]=ice;cells+=1
    continue
   peak=62+int(h*(1-rad/max(edge,1))**.65+.7*np.sin(dx*.6+dz*.3))
   bottom=62-int(4+7*(1-rad/max(edge,1)))
   for y in range(bottom,max(63,peak)+1):
    if q[iz,y,ix] not in [air,water,ice,packed,blue]:continue
    q[iz,y,ix]=blue if y<60 and rad<edge*.6 else packed;cells+=1
   if peak>=65 and ((dx+dz)%4!=0):q[iz,peak,ix]=snow
 bergs.append({'x':cx,'z':cz,'radius':r,'height':h,'blocks':cells})
# Underwater plants occur sparsely in patches, away from the protected harbour.
seagrass=bid('seagrass');kelp=bid('kelp');kelpplant=bid('kelp_plant')
rng=np.random.default_rng(621105);plants=0
for iz,ix in zip(*np.where(cut&(t<54)&(dist>12)&(patch>-.4))):
 if rng.random()>.008:continue
 y=int(t[iz,ix])+1
 if q[iz,y,ix]!=water:continue
 if rng.random()<.16 and y<49:
  length=int(rng.integers(3,9));length=min(length,59-y)
  for yy in range(y,y+length):
   if q[iz,yy,ix]!=water:break
   q[iz,yy,ix]=kelp if yy==y+length-1 else kelpplant;plants+=1
 else:q[iz,y,ix]=seagrass;plants+=1
np.save(P/'grid-target.npy',q);(P/'target-palette.json').write_text(json.dumps(names))
# Exact, non-overlapping vertical runs, sent only through BuildBridge.
jobs=[];counts=collections.Counter();changed=0;bounds=[99999,-99999,99999,-99999,99999,-99999]
for iz in range(H):
 for ix in np.where((q[iz]!=g[iz]).any(axis=0))[0]:
  col=q[iz,:,ix];old=g[iz,:,ix];ys=np.where(col!=old)[0];j=0
  while j<len(ys):
   a=int(ys[j]);b=a;v=int(col[a]);j+=1
   while j<len(ys) and ys[j]==b+1 and col[ys[j]]==v:b=int(ys[j]);j+=1
   jobs.append({'x1':int(ix-1200),'x2':int(ix-1200),'z1':iz+2800,'z2':iz+2800,'y1':a,'y2':b,'block':names[v]})
   n=b-a+1;changed+=n;counts[names[v]]+=n
   bounds=[min(bounds[0],ix-1200),max(bounds[1],ix-1200),min(bounds[2],a),max(bounds[3],b),min(bounds[4],iz+2800),max(bounds[5],iz+2800)]
# Merge adjacent, identical runs into disjoint rectangles to reduce API work.
def merge_runs(items,axis):
 other='z' if axis=='x' else 'x'
 grouped=collections.defaultdict(list)
 for a in items:grouped[(a[other+'1'],a[other+'2'],a['y1'],a['y2'],a['block'])].append(a)
 merged=[]
 for rows in grouped.values():
  rows.sort(key=lambda a:a[axis+'1']);current=None
  for a in rows:
   if current is not None and a[axis+'1']==current[axis+'2']+1:current[axis+'2']=a[axis+'2']
   else:
    if current is not None:merged.append(current)
    current=a.copy()
  if current is not None:merged.append(current)
 return merged
jobs=merge_runs(merge_runs(jobs,'x'),'z')
# Immutable source region copies plus the complete decoded before-grid support recovery.
backup=P/'region-before';backup.mkdir(exist_ok=True)
for rx in range(int(bounds[0])//512,int(bounds[1])//512+1):
 for rz in range(int(bounds[4])//512,int(bounds[5])//512+1):
  src=Path('world/dimensions/minecraft/overworld/region')/f'r.{rx}.{rz}.mca'
  if src.exists() and not (backup/src.name).exists():shutil.copy2(src,backup/src.name)
for n in range(0,len(jobs),4000):(P/f'jobs-{n//4000:03}.json').write_text(json.dumps({'jobs':jobs[n:n+4000]},separators=(',',':')))
# Deterministic random audit samples of changed cells, and unmodified structural cells.
rng=np.random.default_rng(110562);sample=[]
for j in rng.choice(len(jobs),min(750,len(jobs)),replace=False):
 a=jobs[int(j)];y=int(rng.integers(a['y1'],a['y2']+1));x=int(rng.integers(a['x1'],a['x2']+1));z=int(rng.integers(a['z1'],a['z2']+1));sample.append({'x':x,'y':y,'z':z,'before':names[int(g[z-2800,y,x+1200])],'after':a['block']})
structids=[i for i,n in enumerate(meta['names']) if any(k in n for k in ['_planks','_stairs','_slab','_fence','_door','_trapdoor','lantern','iron_chain','barrel','chest','rail'])]
protected=[]
for z in range(2930,3161,3):
 for x in range(-725,-580,3):
  iz=z-2800;ix=x+1200;ys=np.where(np.isin(g[iz,:,ix],structids))[0]
  if len(ys):
   y=int(ys[-1]);protected.append({'x':x,'y':y,'z':z,'block':names[int(g[iz,y,ix])]})
sampledprotect=[protected[i] for i in rng.choice(len(protected),min(250,len(protected)),replace=False)]
(P/'audit-samples.json').write_text(json.dumps({'changes':sample,'protected':sampledprotect}))
# No crafted blocks may be replaced. The entire harbour above Y62 must match byte-for-byte.
changedmask=q!=g;harbour=(xx>=-725)&(zz<=3160)
assert not (changedmask[:,63:,:]&harbour[:,None,:]).any(),'Harbour roof changed'
assert not (changedmask[:,111:,:]).any(),'Upper cliffs changed'
assert not (changedmask[:,63:,:]&roof[:,None,:]).any(),'Overhanging cliff changed'
assert not (changedmask&(~allowed[g])).any(),'Non-natural block changed'
report={'changed_blocks':changed,'jobs':len(jobs),'batches':(len(jobs)+3999)//4000,'bounds':list(map(int,bounds)),'materials':dict(counts),'icebergs':bergs,'plant_blocks':plants,'harbour_structures_unchanged':True,'cliff_roofs_unchanged':True,'protected_audit_samples':len(sampledprotect)}
(P/'write-plan.json').write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
