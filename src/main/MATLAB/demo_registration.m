% registration script v0.2
% Hiep Luong <Hiep.Luong@UGent.be>
% Added features: more generic TIFF-input

% PARAMETERS
search_window = 16;
alpha = 1.0;

close all;

tiff_files = dir('*.tif');
nr = length(tiff_files);

filename = tiff_files(1).name;
im = imread(filename);
[u,v] = size(im);

f1 = warndlg('Choose reference patch in image (2 points)...', 'Patch selection');
figure('Toolbar','figure','Menubar', 'none','Name','EM image','NumberTitle','off','IntegerHandle','off');
imshow(im);
zoom on;
waitfor(f1);
zoom reset;
zoom off;
[y_a,x_a] = ginput(2);

x_a = int16(x_a);
y_a = int16(y_a);

patch_ref = double(im(x_a(1):x_a(2),y_a(1):y_a(2)));
init_x = x_a(1);
init_y = y_a(1);
prev_x = init_x;
prev_y = init_y;
patch_prev = patch_ref;

figure(2);imshow(uint8(patch_ref));
pos_x_list = zeros(1,nr);
pos_y_list = zeros(1,nr);
patch_list = zeros(size(patch_ref,1),size(patch_ref,2),nr);

for i = 1:nr
    im = imread(tiff_files(i).name);
    
    % simple blockmatching (MAD)
    mad_max = 255*(size(patch_ref,1)*size(patch_ref,1));
    min_x = max(prev_x - search_window,1);
    min_y = max(prev_y - search_window,1);
    pos_x = min_x;
    pos_y = min_y;
    for x = min_x:(min_x + 2*search_window + 1)
        for y = min_y:(min_y + 2*search_window + 1)
            patch = double(im(x:(x + size(patch_ref,1) - 1),y:(y + size(patch_ref,2) - 1)));
            diff1 = patch - patch_ref;
            diff2 = patch - patch_prev;
            mad = alpha*sum(abs(diff1(:))) + (1 - alpha)*sum(abs(diff2(:)));
            if (mad < mad_max)
                mad_max = mad;
                pos_x = x;
                pos_y = y;
                patch_temp = patch;
            end
        end
    end
    patch_prev = patch_temp;
    prev_x = pos_x;
    prev_y = pos_y;
    pos_x_list(i) = pos_x;
    pos_y_list(i) = pos_y;
    patch_list(:,:,i) = patch;
    fprintf('Registration displacement (%d/%d): %d - %d\n',i, nr, pos_x - init_x,pos_y - init_y);
end

% refinement
patch_ref_new = median(patch_list,3);
figure(3);imshow(patch_ref_new,[]);
search_window = floor(search_window/4);
pos_x_list2 = zeros(1,nr);
pos_y_list2 = zeros(1,nr);
patch_prev = patch_ref;

warning('OFF');
for i = 1:nr
    im = imread(tiff_files(i).name);
    
    % simple blockmatching (MAD)
    mad_max = 255*(size(patch_ref,1)*size(patch_ref,1));
    prev_x = pos_x_list(i);
    prev_y = pos_y_list(i);
    min_x = max(prev_x - search_window,1);
    min_y = max(prev_y - search_window,1);
    pos_x = min_x;
    pos_y = min_y;
    for x = min_x:(min_x + 2*search_window + 1)
        for y = min_y:(min_y + 2*search_window + 1)
            patch = double(im(x:(x + size(patch_ref,1) - 1),y:(y + size(patch_ref,2) - 1)));
            diff1 = patch - patch_ref_new;
            diff2 = patch - patch_prev;
            mad = alpha*sum(abs(diff1(:))) + (1 - alpha)*sum(abs(diff2(:)));
            if (mad < mad_max)
                mad_max = mad;
                pos_x = x;
                pos_y = y;
                patch_temp = patch;
            end
        end
    end
    patch_prev = patch_temp;
    pos_x_list2(i) = pos_x;
    pos_y_list2(i) = pos_y;
    
    % regridding
    im = padarray(im,[128 128],0,'both');
    im = circshift(im,[init_x - pos_x,init_y - pos_y]);
    %im = im(129:(128+u),129:(128+v));
    imwrite(im,strcat('registered/',tiff_files(i).name));
    
    fprintf('Registration displacement (%d/%d): %d - %d\n',i, nr, pos_x - init_x,pos_y - init_y);
    figure(1);imshow(im);
end
warning('ON');

% evaluation
figure;imshow(uint8(squeeze(patch_list(int8(round(size(patch_list,1)/2)),:,:))));