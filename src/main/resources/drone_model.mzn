int: n;        % number of drones being considered.
float: distance;   % distance to dest. = 450m.
float: weight;     % weight of payload = 1kg.
enum drone_attr;   % [Range, Capacity], used to access elements of drones array

array[1..n, drone_attr] of float: drones;
var float: r;
var int: drone_index;

% check for given drone having minimum requirements for this mission
predicate satisfies_reqs(float: range, float: cap, int: index) = range > (distance*2) /\ cap >= weight -> r = range /\ drone_index = index;

constraint  % check capabilities of each drone passed 
  forall(i in 1..n)( 
    satisfies_reqs(drones[i,Range], drones[i,Capacity], i)
  );
constraint r > 0.0;

output [  % Output CSV string formatted as [drone_index, Range, Capacity]
  "\(drone_index-1),\(drones[drone_index,Range]),\(drones[drone_index,Capacity])\n" % drone_index decremented to match scala standard list indexing
];

solve minimize r;
