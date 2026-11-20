import json,collections,math,shutil
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
from scipy.interpolate import splprep,splev
from PIL import Image,ImageDraw
P=Path('tools/mcwws-build-bridge-mcp/island-east-blend-20261105');meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');base=g.copy();H,W=g.shape[0],g.shape[2];zz,xx=np.mgrid[2970:2970+H,-650:-650+W]
old=np.load('tools/mcwws-build-bridge-mcp/island-repair-20261105/target.npy',mmap_mode='r');oldnames=json.loads(Path('tools/mcwws-build-bridge-mcp/purple-coast-expansion-20261105/target-palette.json').read_text());originalIsland=np.load('tools/mcwws-build-bridge-mcp/island-repair-20261105/mask.npz')['island'][30:30+H,270:270+W]
def bid(n):
 if not n.startswith('minecraft:'):n='minecraft:'+n
 if n not in ids:ids[n]=len(names);names.append(n)
 return ids[n]
markers=json.loads((P/'red-markers.json').read_text())
for a in markers:base[a['z']-2970,a['y']+64,a['x']+650]=bid(oldnames[int(old[a['z']-2940,a['y']+64,a['x']+920])])
water,air,stone,dirt,grass,sand,sandstone=[bid(n) for n in ['water','air','stone','dirt','grass_block','sand','smooth_sandstone']]
# Fit the actual red spline and add bounded, slow coastline variations.
path=np.asarray(json.loads((P/'ordered-red.json').read_text()),float);arc=np.r_[0,np.cumsum(np.linalg.norm(np.diff(path,axis=0),axis=1))];tck,u=splprep(path.T,u=arc/arc[-1],s=8,k=3);ts=np.linspace(0,1,1200);curve=np.asarray(splev(ts,tck)).T;der=np.asarray(splev(ts,tck,der=1)).T;normal=np.column_stack([-der[:,1],der[:,0]]);normal/=np.linalg.norm(normal,axis=1)[:,None]
coastnoise=(1.0*np.sin(ts*math.pi*14+.6)+.65*np.sin(ts*math.pi*26+2.8))*np.sin(np.pi*ts)**.45
curve+=normal*coastnoise[:,None]
im=Image.new('1',(W,H));ImageDraw.Draw(im).polygon([(float(x+650),float(z-2970)) for x,z in curve],fill=1);lobe=np.array(im,bool)
a,b=path[0],path[-1];lo,hi=sorted([a,b],key=lambda p:p[1]);seamX=np.interp(zz,[lo[1],hi[1]],[lo[0],hi[0]])
west=originalIsland&(xx<=seamX)
finalLand=west|lobe
soilnames=['grass_block','dirt','coarse_dirt','rooted_dirt','dirt_path','green_concrete_powder','podzol'];soilids=[bid(n) for n in soilnames]
soils=np.isin(base[:,127:150,:],soilids);soilH=np.where(soils,np.arange(63,86)[None,:,None],-1).max(axis=1)
preserveLeft=originalIsland&(xx<=-570)
finalLand|=preserveLeft
groundnames=['stone','andesite','diorite','granite','light_gray_concrete','gray_concrete','gravel','sand','sandstone','smooth_sandstone','calcite','tuff','deepslate']+soilnames
groundids=[bid(n) for n in groundnames]+[i for i,n in enumerate(names) if n.endswith('_ore')]
first=np.full((H,W),-1,np.int16);run=np.zeros((H,W),np.int16)
for y in range(63,112):
 free=~np.isin(base[:,y+64,:],groundids);run=np.where(free,run+1,0);hit=(first<0)&(run>=4);first[hit]=y-3
surface=np.maximum(first-1,62);surface=np.where(soilH>=68,np.maximum(surface,soilH),surface).astype(np.int16)
for y in range(63,95):
 extend=(surface==y-1)&np.isin(base[:,y+64,:],groundids);surface[extend]=y
airids=[bid(n) for n in ['air','cave_air','void_air']];roof=np.full((H,W),320,np.int16)
for y in range(63,150):
 solid=np.isin(base[:,y+64,:],groundids);hit=(y>surface)&solid&(roof==320);roof[hit]=y
# Reference profiles retain their actual coherent material patches and strata.
reference=(xx>=-618)&(xx<=-570)&(zz>=3034)&(zz<=3108)&(soilH>=68)&(soilH<=83)
patchRef=reference[64:139,32:81];srcH=soilH[64:139,32:81].copy();near=ndi.distance_transform_edt(~patchRef,return_distances=False,return_indices=True);srcH[~patchRef]=srcH[tuple(near[:,~patchRef])]
srcTop=base[np.mgrid[64:139,32:81][0],srcH+64,np.mgrid[64:139,32:81][1]]
srcDepth=np.ones_like(srcH)
for z in range(75):
 for x in range(49):
  hh=int(srcH[z,x]);d=0
  while d<12 and base[z+64,hh-d+64,x+32] in soilids:d+=1
  srcDepth[z,x]=np.clip(d,2,6)
rng=np.random.default_rng(591105);fuzz=ndi.gaussian_filter(rng.normal(size=(H,W)),5);fuzz/=fuzz.std()
sz=(zz-3034)*.95+fuzz*.9;sx=48-(xx+570)*.85+ndi.gaussian_filter(fuzz,4)*1.1
sourceHeight=ndi.map_coordinates(srcH.astype(float),[sz,sx],order=1,mode='reflect')
sourceTop=ndi.map_coordinates(srcTop,[sz,sx],order=0,mode='reflect').astype(np.uint16);sourceDepth=ndi.map_coordinates(srcDepth,[sz,sx],order=0,mode='reflect')
# Use reflected source coordinates to sample the same rock patterns under the soil.
sourceZ=ndi.map_coordinates(np.tile(np.arange(75)[:,None],(1,49)).astype(float),[sz,sx],order=1,mode='reflect')+64
sourceX=ndi.map_coordinates(np.tile(np.arange(49)[None,:],(75,1)).astype(float),[sz,sx],order=1,mode='reflect')+32
inside=ndi.distance_transform_edt(finalLand);outside=ndi.distance_transform_edt(~finalLand)
blend=np.clip(inside/9,0,1);blend=blend*blend*(3-2*blend)
desired=63+(sourceHeight-63)*blend+.35*fuzz*np.minimum(inside/15,1)
desired=np.minimum(desired,roof-4);desired=np.maximum(desired,63)
assetids=[i for i,n in enumerate(names) if any(k in n for k in ['_planks','_stairs','_slab','_fence','_door','_trapdoor','lantern','iron_chain','barrel','chest','rail','_banner','iron_bars','_head','_skull'])]
assets=np.isin(base[:,127:159,:],assetids).any(axis=1);protect=ndi.distance_transform_edt(~assets)<=3
treeids=[i for i,n in enumerate(names) if n.endswith(('_log','_wood'))];treeCols=np.isin(base[:,127:159,:],treeids).any(axis=1);treeAnchor=ndi.distance_transform_edt(~treeCols)<=2
fixed=preserveLeft|protect|treeCols
desired[fixed]=surface[fixed]
# Smooth the join into fixed western profiles, keeping the sampled small relief.
mutable=finalLand&~fixed
h=desired.copy()
for i in range(220):
 nei=ndi.convolve(h,np.array([[0,1,0],[1,0,1],[0,1,0]],float),mode='nearest');new=(nei+.6*desired)/4.6;h[mutable]=new[mutable]
for i in range(70):
 lower=ndi.maximum_filter(h,footprint=np.array([[0,1,0],[1,0,1],[0,1,0]],bool),mode='nearest')-1.6
 raised=np.minimum(np.maximum(h,lower),roof-2);h[mutable]=raised[mutable]
h=np.rint(h).astype(np.int16);h[fixed]=surface[fixed]
truewater=base[:,126,:]==water;solidids=groundids+[i for i,n in enumerate(names) if n.endswith('_ore')];bed=np.where(np.isin(base[:,:126,:],solidids),np.arange(-64,62)[None,:,None],-65).max(axis=1)
validSea=truewater&~originalIsland;_,ind=ndi.distance_transform_edt(~validSea,return_indices=True);seaBase=bed[tuple(ind)]
skirt=np.maximum(seaBase,61-1.5*outside);skirtWeight=np.clip(1-outside/24,0,1);skirtWeight=skirtWeight*skirtWeight*(3-2*skirtWeight)
seaH=np.rint(seaBase*(1-skirtWeight)+skirt*skirtWeight).astype(np.int16);seaH=np.minimum(seaH,60)
extent=(lobe|originalIsland)&(xx>=seamX-7)
extent &= ~preserveLeft
domain=(ndi.distance_transform_edt(~extent)<24)&~preserveLeft&~protect
domain[:2,:]=False;domain[-2:,:]=False;domain[:,:2]=False;domain[:,-2:]=False
target=np.where(finalLand,h,seaH);target[~domain]=surface[~domain]
q=base.copy()
natural={'air','cave_air','void_air','water','bubble_column','stone','andesite','diorite','granite','deepslate','tuff','calcite','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','sand','sandstone','smooth_sandstone','gravel','clay','mud','packed_mud','blackstone','smooth_basalt','basalt','moss_block','moss_carpet','dripstone_block','pointed_dripstone','sculk','sculk_vein','sculk_sensor','sculk_catalyst','sculk_shrieker','amethyst_block','budding_amethyst','magma_block','seagrass','tall_seagrass','kelp','kelp_plant','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','brown_mushroom','red_mushroom','short_grass','tall_grass','fern','large_fern','short_dry_grass','tall_dry_grass','azalea','flowering_azalea','small_dripleaf','big_dripleaf','big_dripleaf_stem','sea_pickle','powder_snow','snow','snow_block','spore_blossom','cobweb','raw_copper_block','raw_iron_block','bone_block','dead_bush','lily_pad','pale_moss_block','pale_moss_carpet','leaf_litter','sweet_berry_bush','light_gray_concrete','gray_concrete','green_concrete_powder','rose_bush','firefly_bush'}
allowed=np.array([n.removeprefix('minecraft:') in natural or n.endswith('_ore') for n in names])
rockids={bid(n) for n in ['stone','andesite','diorite','granite','light_gray_concrete','gravel','sand','sandstone','smooth_sandstone','calcite','tuff','deepslate']}
rockids.update(i for i,n in enumerate(names) if n.endswith('_ore'))
def setone(iz,iy,ix,value):
 if allowed[q[iz,iy,ix]]:q[iz,iy,ix]=value
for iz,ix in zip(*np.where(domain)):
 top=int(target[iz,ix]);oldtop=int(surface[iz,ix]);szi=int(round(sourceZ[iz,ix]));sxi=int(round(sourceX[iz,ix]));sh=int(sourceHeight[iz,ix]);dep=int(sourceDepth[iz,ix])
 if finalLand[iz,ix]:
  if inside[iz,ix]<3:dep=1
  elif inside[iz,ix]<7:dep=min(dep,2)
  start=min(int(bed[iz,ix])+1,top-18)
  for y in range(max(2,start),top+1):
   dy=top-y;sy=np.clip(sh-dy+64,0,383);v=int(base[szi,sy,sxi])
   if dy<dep:
    if dy==0:v=int(sourceTop[iz,ix])
    elif v not in soilids:v=dirt
   elif v not in rockids:v=stone
   if inside[iz,ix]<2:
    v=sand if dy<2 else sandstone if dy<4 else v
   setone(iz,y+64,ix,v)
 else:
  oldbed=int(bed[iz,ix])
  if top>oldbed:
   for y in range(max(2,oldbed+1),top+1):setone(iz,y+64,ix,bid('gravel') if y==top else stone)
  for y in range(max(2,top-2),top+1):setone(iz,y+64,ix,bid('gravel') if y==top else stone)
  for y in range(top+1,63):setone(iz,y+64,ix,water)
 clear=min(94,int(roof[iz,ix])-1,max(oldtop+2,top+2))
 for y in range(max(top+1,63),clear+1):setone(iz,y+64,ix,air)
# Native trees: compact voxel templates sampled from the west, scaled to fit the cave.
woodids=[i for i,n in enumerate(names) if n.endswith('_wood')];leafids=[i for i,n in enumerate(names) if n.endswith('_leaves')]
sub=base[60:87,137:177,60:87];m=np.isin(sub,woodids+leafids);labels,n=ndi.label(m,np.ones((3,3,3)));templates=[]
for c in range(1,n+1):
 z,y,x=np.where(labels==c);w=np.isin(sub[z,y,x],woodids)
 if len(x)<80 or not w.any():continue
 bx,bz=float(np.mean(x[w])),float(np.mean(z[w]));ground=75
 pts={}
 for zi,yi,xi in zip(z,y,x):
  v=int(sub[zi,yi,xi]);dx=int(round((xi-bx)*.72));dz=int(round((zi-bz)*.72));dy=int(round((yi+73-ground)*.55));key=(dx,dy,dz)
  if key not in pts or v in woodids:pts[key]=v
 templates.append([(dx,dy,dz,v) for (dx,dy,dz),v in pts.items()])
 if len(templates)>=2:break
newtree=[];positions=[];newgrass=domain&finalLand&(inside>11)&(target>=71)&~fixed
candidates=list(zip(*np.where(newgrass)));rng.shuffle(candidates)
existingTreeDistance=ndi.distance_transform_edt(~treeCols)
treecount=min(7,max(2,int(newgrass.sum()/650)))
for iz,ix in candidates:
 if len(newtree)>=treecount or not templates:break
 if existingTreeDistance[iz,ix]<12 or any((iz-a)**2+(ix-b)**2<14**2 for a,b in positions):continue
 if names[int(q[iz,int(target[iz,ix])+64,ix])].removeprefix('minecraft:') not in ['grass_block','coarse_dirt','rooted_dirt','dirt']:continue
 pts=templates[len(newtree)%len(templates)];gh=int(target[iz,ix]);out=[];valid=True
 for dx,dy,dz,v in pts:
  if len(newtree)>=4:dx=-dx
  for _ in range(len(newtree)%4):dx,dz=-dz,dx
  z,x=iz+dz,ix+dx;y=gh+dy
  if not (0<=z<H and 0<=x<W) or not finalLand[z,x] or y>=min(95,int(roof[z,x])-1) or y<gh-2:valid=False;break
  if preserveLeft[z,x] or protect[z,x]:valid=False;break
  oldv=int(q[z,y+64,x]);name=names[oldv]
  if dy>=1 and name not in ['minecraft:air','minecraft:cave_air','minecraft:short_dry_grass','minecraft:fern','minecraft:tall_grass','minecraft:short_grass'] and oldv not in leafids:valid=False;break
  out.append((z,y+64,x,v))
 if not valid:continue
 for z,y,x,v in sorted(out,key=lambda a:a[3] not in woodids):q[z,y,x]=v
 positions.append((iz,ix));newtree.append({'x':ix-650,'z':iz+2970,'ground':gh,'blocks':len(out)})
# Ground cover uses the same species and density observed on the left.
covercounts=collections.Counter();coverChoices=[('fern',.059),('short_dry_grass',.045),('tall_grass',.024),('rose_bush',.0045),('firefly_bush',.0005)]
for iz,ix in zip(*np.where(domain&finalLand&(inside>4)&~fixed)):
 h=int(target[iz,ix]);cap=int(q[iz,h+64,ix]);r=rng.random();acc=0;chosen=None
 if cap not in [ids['minecraft:'+n] for n in ['grass_block','coarse_dirt','rooted_dirt','dirt','podzol'] if 'minecraft:'+n in ids]:continue
 for plant,prob in coverChoices:
  acc+=prob
  if r<acc:chosen=plant;break
 if chosen is None or h+2>=min(95,int(roof[iz,ix])-1):continue
 if q[iz,h+65,ix]!=air:continue
 value=bid(chosen)
 if len(allowed)<len(names):allowed=np.pad(allowed,(0,len(names)-len(allowed)),constant_values=True)
 if chosen in ['tall_grass','rose_bush']:
  if q[iz,h+66,ix]!=air:continue
  q[iz,h+65,ix]=value;q[iz,h+66,ix]=value
 else:q[iz,h+65,ix]=value
 covercounts[chosen]+=1
# Metadata is assigned in the placement file (plant halves and persistent foliage).
np.save(P/'grid-target.npy',q);np.savez_compressed(P/'design.npz',lobe=lobe,final_land=finalLand,domain=domain,preserve_left=preserveLeft,protect=protect,target=target,surface=surface,roof=roof,inside=inside,curve=curve,original_island=originalIsland)
(P/'target-palette.json').write_text(json.dumps(names))
assert np.array_equal(q[:,159:,:],g[:,159:,:]),'Cave roof changed'
assert not ((q!=g)&preserveLeft[:,None,:]&(g!=ids['minecraft:red_wool'])).any(),'Left island changed'
assert not ((q!=g)&protect[:,None,:]&(g!=ids['minecraft:red_wool'])).any(),'Assets changed'
change=np.where(q!=g);z,y,x=change
states=[];sid={};records=[];samples=[];sampleindices=set(map(int,rng.choice(len(x),min(1000,len(x)),replace=False)))
for n,(iz,iy,ix) in enumerate(zip(z,y,x)):
 name=names[int(q[iz,iy,ix])];state=name
 if name.endswith('_leaves'):state+='[persistent=true,distance=1,waterlogged=false]'
 elif name in ['minecraft:tall_grass','minecraft:rose_bush']:state+='[half='+('upper' if iy>0 and q[iz,iy-1,ix]==q[iz,iy,ix] else 'lower')+']'
 if state not in sid:sid[state]=len(states);states.append(state)
 records.append((int(ix),int(iy),int(iz),sid[state]))
 if n in sampleindices:samples.append({'x':int(ix-650),'y':int(iy-64),'z':int(iz+2970),'before':names[int(g[iz,iy,ix])],'after':state})
np.asarray(records,dtype='<u2').tofile(P/'placements.u16');(P/'states.json').write_text(json.dumps(states))
backup=P/'region-before';backup.mkdir(exist_ok=True)
bounds=[int(x.min()-650),int(x.max()-650),int(y.min()-64),int(y.max()-64),int(z.min()+2970),int(z.max()+2970)]
for rx in range(bounds[0]//512,bounds[1]//512+1):
 for rz in range(bounds[4]//512,bounds[5]//512+1):
  src=Path('world/dimensions/minecraft/overworld/region')/f'r.{rx}.{rz}.mca'
  if not (backup/src.name).exists():shutil.copy2(src,backup/src.name)
protected=[]
for mask,label,count in [(preserveLeft,'left',180),(protect,'assets',60)]:
 zz0,xx0=np.where(mask)
 for j in rng.choice(len(xx0),min(count,len(xx0)),replace=False):
  iz,ix=zz0[j],xx0[j];iy=int(surface[iz,ix])+64
  if g[iz,iy,ix]!=q[iz,iy,ix]:continue
  protected.append({'x':int(ix-650),'y':int(iy-64),'z':int(iz+2970),'block':names[int(g[iz,iy,ix])],'role':label})
for j in rng.choice(len(x),120,replace=False):
 iz,ix=z[j],x[j];iy=int(min(319,max(95,roof[iz,ix])))+64;protected.append({'x':int(ix-650),'y':iy-64,'z':int(iz+2970),'block':names[int(g[iz,iy,ix])],'role':'roof'})
profiles=[]
for iz,ix in zip(*np.where(domain)):
 if (ix*17+iz*23)%47:continue
 h=int(target[iz,ix]);
 for yy in [h-1,h,h+1,62]:profiles.append({'x':int(ix-650),'y':yy,'z':int(iz+2970),'block':names[int(q[iz,yy+64,ix])]})
red_checks=[{'x':a['x'],'y':a['y'],'z':a['z'],'before':'minecraft:red_wool','after':names[int(q[a['z']-2970,a['y']+64,a['x']+650])]} for a in markers]
(P/'audit.json').write_text(json.dumps({'changes':samples,'protected':protected,'profiles':profiles,'red':red_checks}))
report={'placements':len(records),'bounds':bounds,'target_land_columns':int(finalLand.sum()),'edited_columns':int(domain.sum()),'western_columns_preserved':int(preserveLeft.sum()),'new_trees':newtree,'ground_cover':dict(covercounts),'red_markers_removed':len(markers),'coast_noise_max':float(np.abs(coastnoise).max()),'materials':dict(collections.Counter(names[int(q[iz,iy,ix])] for iz,iy,ix in zip(z,y,x)))}
(P/'write-plan.json').write_text(json.dumps(report,indent=2,default=int));print(json.dumps(report,default=int),flush=True)
rgb=np.full((H,W,3),[25,70,115],np.uint8);rgb[originalIsland]=[90,90,100];rgb[finalLand]=[70,125,60];rgb[preserveLeft]=[40,95,40];rgb[protect]=[190,130,50]
for cx,cz in curve:
 ix=int(round(cx+650));iz=int(round(cz-2970))
 if 0<=iz<H and 0<=ix<W:rgb[iz,ix]=[220,35,35]
im=Image.fromarray(rgb).resize((W*6,H*6));dr=ImageDraw.Draw(im)
for xx0 in range(-650,-454,20):
 px=(xx0+650)*6;dr.line((px,0,px,H*6),fill=(65,65,70));dr.text((px+2,5),str(xx0),fill='white')
for zz0 in range(2970,3156,20):
 pz=(zz0-2970)*6;dr.line((0,pz,W*6,pz),fill=(65,65,70));dr.text((2,pz+2),str(zz0),fill='white')
im.save(P/'plan.png')
