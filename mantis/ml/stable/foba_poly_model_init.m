%function [num_orig_feats] = foba_poly_model_init(dir_input, key, runs)

%% Parsing argument list
arg_list = argv ();
%for i = 1:nargin
%    printf ('%s\n', arg_list{i});
%end
dir_input = arg_list{1};
key = '';
runs = str2num(arg_list{2});

%% Input files
file_time   = strcat(dir_input, key, '/exectime.mat');
file_data   = strcat(dir_input, key, '/feature_data.mat');
file_var    = strcat(dir_input, key, '/varying_features.mat');
file_costly = strcat(dir_input, key, '/costly_features.txt');

%% Reading data
% Execution time file
if (runs == 1)
    file_name = strcat(dir_input, key, '/exectime', '.txt');
    runtime = load(file_name);
else
    for i = 1:runs
        file_name = strcat(dir_input, key, '/exectime', int2str(i), '.txt');
        raw_time(:, i) = load(file_name);
    end
    runtime = sum(raw_time, 2)/runs;
end
% Cost file
file_costs = strcat(dir_input, key, '/feature_cost.txt');
costs = load(file_costs);
costs = costs';
% Feature file
file_feats = strcat(dir_input, key, '/feature_data.txt');
raw_data = load(file_feats);
raw_data = raw_data';
num_orig_feats = size(raw_data, 2);

%% Intermediate files
% Get features with variability
var_data = var(raw_data);
var_f = find(var_data > 0);
% Remove redudant features
[org_data, unique_f] = remove_identical_cols(raw_data(:, var_f), costs(var_f));
var_f = var_f(unique_f);

raw_data = normalization(raw_data);
var_data = raw_data(:, var_f);
costly_f = [0];

% For storing intermediate results
save(file_time, 'runtime');
save(file_data, 'var_data');
save(file_var, 'var_f', 'num_orig_feats');

% For costly features, initialized to be empty file.
fid = fopen(file_costly, 'w');
fprintf(fid, '%d ', costly_f);
fclose(fid);

