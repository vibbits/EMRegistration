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
    xmin = x-1
    xmax = x+4 
    ymin = y-3
    ymax = y+1

    % Allocate space for return values
    % (Hack to be able to use input arguments for returning values)
    best_pos = zeros(1, 2)
    best_patch = zeros(size(ref_patch))
    
    registration(img, ref_patch, xmin, xmax, ymin, ymax, best_pos, best_patch)
    [best_y, best_x] = best_pos
    printf("The best match was found at (y,x)=(%d,%d) in the original image. The patch there is", best_y, best_x)
    print(best_patch)
    
    assert(best_y == y)
    assert(best_x == x)
end

% Input
% - im: slice to find the ref_patch in
% - ref_patch: matrix with pixels of the reference patch
% - x_min, x_max, y_min, y_max: search window, range of x and y for the top-left coordinates of a possible matching block;
%    note (0,0) is the top-left corner pixel of 'img'.
% Output 
% - best_pos: array of 2 scalars with (x,y); position of left-top corner of patch wrt top-left corner of image
%             (passed as argument as a workaround for returning it as a return variable which is problematic for the JavaQuasarBridge)
% - best_patch: matrix with pixels of the best matching patch for the reference patch
%
function [] = registration(img, ref_patch, xmin, xmax, ymin, ymax, best_pos, best_patch)
	assert(xmin <= xmax)
	assert(ymin <= ymax)
	
    patch_height = size(ref_patch, 0)
	patch_width = size(ref_patch, 1)
	printf("Registration xmin=%d xmax=%d ymin=%d ymax=%d / patch width=%d height=%d", xmin, xmax, ymin, ymax, patch_width, patch_height)
    
    %imwrite("E:\\junk_img.png", img)
    %imwrite("E:\\junk_ref_patch.png", ref_patch)
	
	mad = zeros(ymax-ymin+1, xmax-xmin+1)
	for x = xmin..xmax
		for y = ymin..ymax
			patch = img[y..y+patch_height-1, x..x+patch_width-1]
			diff = patch - ref_patch
			mad[y-ymin, x-xmin] = sum(abs(diff))
            % printf("%d %d -> diff=%f", x, y, sum(abs(diff)))
		end
	end

    j = find_index_min(mad)
	pos = ind2pos(size(mad), j)
    % pos=[i,j] indices in the mad array where the best (=lowest) cost is;
    % i and j are 0-based indices; i is measured top to bottom, j left to right
    % this position also implicitly defines the coordinates with respect to the image where the best match was found
    
    best_cost = mad[pos]
    %print(mad)
	%print(j)
    %print(pos)
    
	best_y = ymin + pos[0]
	best_x = xmin + pos[1]

    % Fill in the return values
    % Note that for this to work we must fill the *contents* of these arrays,
    % we can NOT simply assign one array to another. So for example:
    %    best_patch = img[...]
    % does not work; it has to be
    %    best_patch[...] = img[...]
    best_pos[0] = best_y
	best_pos[1] = best_x	
	best_patch[0..patch_height-1, 0..patch_width-1] = img[best_y .. best_y+patch_height-1, best_x .. best_x+patch_width-1]
	
	%imwrite("E:\\junk_best_patch.png", best_patch)
	
    printf("Best matching position [y,x] = [%d,%d], cost = %f\n", best_pos[0], best_pos[1], best_cost);
end
