
import "TIFFIO.dll"
import "system.q"

%
% The main function - for testing the code
%
function [] = main()

    % read data
    img = tiffread("test_stack.tif", 0..511).slices
    
    % parameters
    n = 100      % index of the reference slice
    n_cmp = 4   % number of slices to compare
    alpha = 1   % damping parameter (high values give more importance to slices further away and vice versa)
    
    % select reference slice and slices to compare
    img_ref = img[n]
    img_cmp = zeros(size(img_ref,0), size(img_ref,0), n_cmp)
    weights = zeros(n_cmp)
    for i=0..n_cmp-1
        img_cmp[:,:,i] = img[n+2^i]     % select 1, 2, 4, 8, 16, ... slices further
        weights[i] = exp(-i/alpha)      % importance weights follow exponential decay
    end
    
    % register the reference slice
    [dx, dy] = register_mse(img_ref, img_cmp, weights)
    print([dx, dy])
    
    % show registered image
    img_reg = circshift(img_ref, [dx, dy])
    f = imshow(img_ref)
    g = imshow(img_reg)
    f.connect(g)
    
end

% Function: register
% 
% This function registers a slice (translations only) by comparing it to a list of other slices. 
% The similarity criterion is MSE
% 
% Usage:
%   : function [x_reg : mat] = register_mse(x_ref : mat'const, x_cmp : cube'const, weights : vec'const)
% 
% Parameters:
% x_ref - a reference slice
% x_cmp - a set of slices that should be compared to the reference slice (stacked as a cube)
% weights - a weight for each slice indicating its importance (e.g. closeness to the reference slice)
% h - half search window size
% 
% Returns:
% dx - the vertical shift
% dy - the horizontal shift
function [dx:int, dy:int] = register_mse(x_ref:mat, x_cmp:cube, weights:vec, h:int=20)

    % allocate variables
    n_cmp = size(x_cmp, 2)
    
    % make sure the weights are normalized (i.e. sum to 1)
    weights = weights./sum(weights)
    
    % perform exhaustive registration on the complete image
    dx_ncmp = zeros(n_cmp)
    dy_ncmp = zeros(n_cmp)
    for n=0..n_cmp-1
        [dx, dy] = register_exhaustive(x_ref, x_cmp[:,:,n], h)
        dx_ncmp[n] = dx
        dy_ncmp[n] = dy
    end
    dx = int(sum(dx_ncmp.*weights))
    dy = int(sum(dy_ncmp.*weights))
    
end

% Function: register_exhaustive
% 
% This function registers a slice (translations only) to another slice. 
% The similarity criterion is MSE
% 
% Usage:
%   : function [dx_opt : int, dy_opt : int] = register_exhaustive(x_ref : mat'const, x_cmp : mat'const, h : int'const, dx_init : int'const = 0, dy_init : int'const = 0)
% 
% Parameters:
% x_ref - a reference slice
% x_cmp - a set of slices that should be compared to the reference slice (stacked as a cube)
% h - half search window size
% dx_init - initialization of the vertical translation (optional)
% dy_init - initialization of the horizontal translation (optional)
% 
% Returns:
% dx_opt - the vertical shift
% dy_opt - the horizontal shift
function [dx_opt:int, dy_opt:int] = register_exhaustive(x_ref:mat, x_cmp:mat, h:int, dx_init:int=0, dy_init:int=0)

    % set initial optimal translation and MSE
    dx_opt = 0
    dy_opt = 0
    mse_min = 255^2
    
    % loop over all shifts
    for dx=dx_init-h..dx_init+h
    for dy=dy_init-h..dy_init+h
    
        % extract shifted image
        x_ref_d = circshift(x_ref, [dx,dy])
            
        % compute mse
        mse = mean((x_ref_d - x_cmp).^2)
        
        % update optimal translation if necessary
        if mse < mse_min
            mse_min = mse
            dx_opt = dx
            dy_opt = dy
        endif
    
    end
    end
    
end