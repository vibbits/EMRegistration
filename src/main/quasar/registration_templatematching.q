% Image registration by simple patch matching.
% Original implementation in MATLAB by Hiep Luong,
% Quasar version by Frank Vernaillen and Joris Roels.
% March 2018.
import "parallelblocks.q"

function [] = main()
    % Create our reference patch
    int pw = 5
    int ph = 2  
    ref_patch = 200 * ones(ph, pw)
    print(ref_patch)

    % Create a blank image
    int ih = 10
    int iw = 20 
    img = zeros(ih, iw)

    % Write the reference patch into image	
	x = 3
	y = 6
 	img[y .. y+ph-1, x..x+pw-1] = ref_patch
	print(img)
    
    % Add a bit of noise to the image
    img = img + randn(ih, iw)
    print(img)
    
    % See if we can find the reference patch back in the noisy image
    xmin = x-2
    xmax = x+10 
    ymin = y-5
    ymax = y+1

    % Allocate space for return values
    best_pos = zeros(1, 2)
    
    registration(img, ref_patch, xmin, xmax, ymin, ymax, best_pos)
    [best_y, best_x] = best_pos
    printf("The best match was found at (y,x)=(%d,%d) in the original image.", best_y, best_x)

    % Extract and print best_patch (for testing/debugging)
    best_patch = img[best_y..best_y+size(ref_patch,0)-1, best_x..best_x+size(ref_patch,1)-1]
    print(best_patch)

    % Compare found patch position to ground truth
    assert(best_y == y)
    assert(best_x == x)
end

% Input
% - img: image to find ref_patch in
% - ref_patch: reference image patch 
% - xmin, xmax, ymin, ymax: search window, range of x and y for the top-left coordinates of a possible matching block;
%    The top-left corner of 'img' is the origin, with y-axis pointing down, and the x-axis pointing to the right. 
% Output 
% - best_pos: array of 2 scalars (y,x); position of left-top corner of patch wrt top-left corner of image
%             (The result is passed as an *argument*  of registration(). This is a workaround for returning it as a return variable which is problematic for the JavaQuasarBridge)
%
function [] = registration(img, ref_patch, xmin, xmax, ymin, ymax, best_pos)
	assert(xmin <= xmax)
	assert(ymin <= ymax)
	
    patch_height = size(ref_patch, 0)
	patch_width = size(ref_patch, 1)
  % printf("Registration xmin=%d xmax=%d ymin=%d ymax=%d / patch width=%d height=%d", xmin, xmax, ymin, ymax, patch_width, patch_height)
    
	mad = zeros(ymax-ymin+1, xmax-xmin+1)
	for x = xmin..xmax
		for y = ymin..ymax
			patch = img[y..y+patch_height-1, x..x+patch_width-1]
			diff = patch - ref_patch
			mad[y-ymin, x-xmin] = sum(abs(diff))
		end
	end

    j = find_index_min(mad)
	pos = ind2pos(size(mad), j)
    % pos=[i,j] indices in the mad array where the best (=lowest) cost is;
    % i and j are 0-based indices; i is measured top to bottom, j left to right
    % this position also implicitly defines the coordinates with respect to the image where the best match was found
    
  % best_cost = mad[pos] % not used
	best_y = ymin + pos[0]
	best_x = xmin + pos[1]

    % Fill in the return value
    % Note that for this to work we must fill the *contents* of the array,
    % we can NOT simply assign one array to another.
    best_pos[0] = best_y
	best_pos[1] = best_x	
end
