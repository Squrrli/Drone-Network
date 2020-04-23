int: n;        % number of drones being considered.
float: distance;   % distance to dest. = 450m.
float: weight;     % weight of payload = 1kg.
enum drone_attr;   % [Range, Capacity], used to access elements of drones array

array[1..n, drone_attr] of float: drones;
array[1..n] of var int: ranges;


% check for given drone having minimum requirements for this mission
predicate satisfies_reqs(float: range, float: cap, int: index) = 
      range >= (distance*1.2) /\ cap >= weight;

constraint  % check capabilities of each drone passed 
  forall(i in 1..n)( 
    ranges[i] = if satisfies_reqs(drones[i,Range], drones[i,Capacity], i)
        then drones[i,Range]
        else 0
        endif
  );

output [  % Output CSV string formatted as [drone_index, Range, Capacity]
  "\(ranges)\n"
];

solve minimize min(ranges);
