#-------------------------------------------------------------------------------
# This file extends PyMol with some custom functions that are called 
# by the CMView application through the PyMol XMLRPC server.
#---------------------------------

from pymol.cgo import *
from pymol import cmd
from math import *
from pymol import string

#-------------------------------------------------
# Create the given file to notify CMView that
# PyMol is running and receiving commands.
# This can be used as a general callback facility
# to send messages from PyMol to CMView.
#---------------------------------
def callback(filename, value):
	'''
	DESCRIPTION
	
	"callback" creates a file containing the given value.
	This is being used as a callback facility by the
	CMView application to check whether PyMol is running
	and receiving commands.
	
	USAGE

	(This function is not supposed to be called manually!)
	
	callback filename, value
	'''

	f = open(filename, "w")
	f.write(value)
	f.close()
	
# Add to PyMOL API
cmd.extend("callback",callback)

#-------------------------------------------------
# Draw an edge
#---------------------------------

def edge(name, i_node, j_node, color = None, r = 1.0, g=0.0, b=0.0, dg = 0.3, dl = 0.5, dr = 0.2, dir = 1, dir_color = None, dir_r = 0.0, dir_g = 1.0, dir_b = 0.0): 
	
	'''
	DESCRIPTION

	"edge" creates a cylinder (actually sausage) between the two
	selections that	correspond to the 2 nodes. If the edge is
	directed, only half of the user-formatted cylinder will be
	drawn towards the target node n2 and the rest will be drawn as
	a thin cylinder. 

	USAGE

	edge name, i_node, j_node [, color, r, g, b, dg, dl, dr, dir,
	dir_color, dir_r, dir_g, dir_b]

	name = name of edge
	i_node, j_node = atom selections for node 1 and node 2
	color = color name (overwrites rgb)
	r, g, b = rgb color (default red)
	dg = dash gap (default 0 - alternative 0.3)
	dl = dash length (default 0.5)
	dr = dash radius (default 0.2)
	dir = directed edge (default 1-yes)
	dir_color = color name for the other half (overwrites dir_rgb)
	dir_[r, g, b] = rgb color for the other half (default green)
	'''
	
	if color is not None:
		 color_rgb =  cmd.get_color_tuple(cmd.get_color_index(color))
		 r = color_rgb[0]
		 g = color_rgb[1]
		 b = color_rgb[2]
	else:
		# Convert arguments into floating point values
		r = float(r)
		g = float(g)
		b = float(b)
		
	if dir_color is not None:
		dir_color_rgb =	cmd.get_color_tuple(cmd.get_color_index(dir_color))
		dir_r = dir_color_rgb[0]
		dir_g = dir_color_rgb[1]
		dir_b = dir_color_rgb[2]
	else:
		dir_r = float(dir_r)
		dir_g = float(dir_g)
		dir_b = float(dir_b)
	
	dg = float(dg)
	dl = float(dl)
	dr = float(dr)
	
	directed = int(dir)
	frag = directed + 1

	# Get tuple containing object and index of atoms in these
	# selections
	x1 = cmd.index(i_node,1)
	x2 = cmd.index(j_node,1)

	# Get number of atoms in each selection
	n1 = len(x1)
	n2 = len(x2)
	if(n1 < 1):
		print "Error: node " + n1 + " has no atoms"
		return
	if(n2 < 1):
		print "Error: node " + n2 + " has no atoms"
		return

	# Get objects and atom indices
	o1 = x1[0][0]
	i1 = x1[0][1]
	o2 = x2[0][0]
	i2 = x2[0][1]

	# Get ChemPy models
	m1 = cmd.get_model(o1)
	m2 = cmd.get_model(o2)

	# Get atoms
	a1 = m1.atom[i1-1]
	a2 = m2.atom[i2-1]

	# Get coords
	x1 = a1.coord[0]
	y1 = a1.coord[1]
	z1 = a1.coord[2]
	x2 = a2.coord[0]
	y2 = a2.coord[1]
	z2 = a2.coord[2]

	# Make some nice strings for user feedback
	#n1 = o1 + "/" + a1.segi + "/" + a1.chain + "/" + a1.resn + "." + a1.resi + "/" + a1.name
	#print n1 + "(" + str(x1) + "," + str(y1) + "," + str(z1) + ")"
	#n2 = o2 + "/" + a2.segi + "/" + "/" + a2.chain + "/" + a2.resn + "." + a2.resi + "/" + a2.name
	#print n2 + "(" + str(x2) + "," + str(y2) + "," + str(z2) + ")"

	# Calculate distances 
	dx = (x2 - x1) / frag
	dy = (y2 - y1) / frag
	dz = (z2 - z1) / frag
	d = math.sqrt((dx*dx) + (dy*dy) + (dz*dz))
	#print "distance = " + str(d) + "A"

	# Work out how many times (dash_len + gap_len) fits into d
	dash_tot = dl + dg
	n_dash = math.floor(d / dash_tot)

	# Work out step lengths
	dx1 = (dl / dash_tot) * (dx / n_dash)
	dy1 = (dl / dash_tot) * (dy / n_dash)
	dz1 = (dl / dash_tot) * (dz / n_dash)
	dx2 = (dx / n_dash)
	dy2 = (dy / n_dash)
	dz2 = (dz / n_dash)

	# Generate dashes
	x = x1
	y = y1
	z = z1

	# Empty CGO object
	obj = []	
	for i in range(n_dash):
	   # Generate a sausage
	   obj.extend([SAUSAGE, x, y, z, x+dx1, y+dy1, z+dz1, dr, r, g, b, r, g, b])
	   
	   # Move to start of next dash
	   x = x + dx2
	   y = y + dy2
	   z = z + dz2

	if directed == 1:
	   obj.extend([SAUSAGE, x, y, z, x2, y2, z2, 0.05, dir_r, dir_g, dir_b, dir_r, dir_g, dir_b])

	cmd.set("stick_quality", 24)
	# Load the object into PyMOL
	cmd.load_cgo(obj, name)
	


# Add to PyMOL API
cmd.extend("edge",edge)

#-------------------------------------------------
# Draw a triangle
#---------------------------------

def triangle(name, i_node, j_node, k_node, contact_type, color, tr): 
	
	'''
	DESCRIPTION

	"triangle" creates a triangle with the given nodes

	USAGE

	triangle name, i_node, j_node, k_node, color, tr

	name = name of triangle
	i_node, j_node, k_node = the residue numbers
	color = color name
	tr = transparency value
	'''
	
	color_rgb =  cmd.get_color_tuple(cmd.get_color_index(color))
	r = color_rgb[0]
	g = color_rgb[1]
	b = color_rgb[2]
	
	str_list = []
	str_list.append(str(i_node))
	str_list.append(str(j_node))
	str_list.append(str(k_node))
	res_str = string.join(str_list, '+')
	str_list[0] = "resi"
	str_list[1] = res_str
	str_list[2] = "and name"
	str_list.append(str(contact_type))
	sel_str = string.join(str_list, ' ')
	#print sel_str

	stored.xyz = []
	cmd.create("triangle", sel_str)
	cmd.iterate_state(1,"triangle","stored.xyz.append([x,y,z])")
	cmd.delete("triangle")
	#print stored.xyz

	#1st way doesn't work
	normalx = ((stored.xyz[1][1]-stored.xyz[0][1])*(stored.xyz[2][2]-stored.xyz[0][2]))-((stored.xyz[2][1]-stored.xyz[0][1])*(stored.xyz[1][2]-stored.xyz[0][2]))
	normaly = ((stored.xyz[1][2]-stored.xyz[0][2])*(stored.xyz[2][0]-stored.xyz[0][0]))-((stored.xyz[2][2]-stored.xyz[0][2])*(stored.xyz[1][0]-stored.xyz[0][0]))
	normalz = ((stored.xyz[1][0]-stored.xyz[0][0])*(stored.xyz[2][1]-stored.xyz[0][1]))-((stored.xyz[2][0]-stored.xyz[0][0])*(stored.xyz[1][1]-stored.xyz[0][1]))
	obj = [
	   BEGIN, TRIANGLES,
	   stored.xyz[0][0], stored.xyz[0][1], stored.xyz[0][2],
	   stored.xyz[1][0], stored.xyz[1][1], stored.xyz[1][2],
	   stored.xyz[2][0], stored.xyz[2][1], stored.xyz[2][2],
	   normalx-stored.xyz[0][0], normaly-stored.xyz[0][1],
	normalz-stored.xyz[0][2],
	   normalx-stored.xyz[1][0], normaly-stored.xyz[1][1],
	normalz-stored.xyz[1][2],
	   normalx-stored.xyz[2][0], normaly-stored.xyz[2][1],
	normalz-stored.xyz[2][2],
	    1.0, 1.0, 1.0,
	    1.0, 1.0, 1.0,
	    1.0, 1.0, 1.0,
	    END
	    ]

	#2nd way
	obj = []
	obj.extend([cgo.ALPHA, tr])
	obj.extend([
	   BEGIN, TRIANGLES,
	   COLOR, r, g, b,
	   VERTEX, stored.xyz[0][0], stored.xyz[0][1], stored.xyz[0][2],
	   VERTEX, stored.xyz[1][0], stored.xyz[1][1], stored.xyz[1][2],
	   VERTEX, stored.xyz[2][0], stored.xyz[2][1], stored.xyz[2][2],
	    END
	    ])
	cmd.load_cgo(obj, name)
	

# Add to PyMOL API
cmd.extend("triangle",triangle)

#-------------------------------------------------
# Draw a sphere
#---------------------------------

def sphere(name, model_and_center_atom, radius, color, tr): 
	
	'''
	DESCRIPTION

	"sphere" creates a sphere with the given center-coordinates and radius

	USAGE

	sphere name, x_center, y_center, z_center, radius, color, tr

	name = name of sphere
	center_atom = center of sphere
	radius = radius of sphere
	color = color name
	tr = transparency value
	'''
	
	color_rgb =  cmd.get_color_tuple(cmd.get_color_index(color))
	r = color_rgb[0]
	g = color_rgb[1]
	b = color_rgb[2]
	
	str_list = []
	#str_list.append(str(center_atom))
	#res_str = str(center_atom)
	#str_list.append(str(model))
	#str_list.append(str("and resi"))
	#str_list.append(str(res_str))
	#str_list.append(str("and name Ca"))
	sel_str = model_and_center_atom #string.join(str_list, ' ')
	print sel_str

	stored.xyz = []
	#stored.xyz.append([x_center,y_center,z_center])
	cmd.create("sphere", sel_str)
	cmd.iterate_state(1,"sphere","stored.xyz.append([x,y,z])")
	cmd.delete("sphere")
	print stored.xyz

	obj = []
	obj.extend([cgo.ALPHA, tr])
	obj.extend([
	   BEGIN, SPHERE,
	   COLOR, r, g, b,
	   SPHERE, stored.xyz[0][0], stored.xyz[0][1], stored.xyz[0][2], radius,	   
	   END
	  ])
	cmd.load_cgo(obj, name)
	

# Add to PyMOL API
cmd.extend("sphere",sphere)