package com.example.assistant.ui.notifications;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentTimePeriodSettingsBinding;
import com.example.assistant.model.TimePeriod;
import com.example.assistant.ui.notifications.MeViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimePeriodSettingsFragment extends Fragment {

    private FragmentTimePeriodSettingsBinding binding;
    private MeViewModel meViewModel;
    private List<com.example.assistant.model.TimePeriod> timePeriods = new ArrayList<>();
    private RecyclerView timePeriodContainer; // 注意这里变量类型改为RecyclerView，但保留原有名称
    private TimePeriodAdapter adapter;

    private class TimePeriodAdapter extends RecyclerView.Adapter<TimePeriodAdapter.TimePeriodViewHolder> {
        
        @NonNull
        @Override
        public TimePeriodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.time_period_item, parent, false);
            return new TimePeriodViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull TimePeriodViewHolder holder, int position) {
            com.example.assistant.model.TimePeriod period = timePeriods.get(position);
            holder.bind(period, position);
        }
        
        @Override
        public int getItemCount() {
            return timePeriods.size();
        }
        
        class TimePeriodViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView;
            TextView timeRangeTextView;
            TextView repeatTextView;
            Switch enableSwitch;
            TextView editArrowButton;
            
            public TimePeriodViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.period_name);
                timeRangeTextView = itemView.findViewById(R.id.time_range);
                repeatTextView = itemView.findViewById(R.id.repeat_days);
                enableSwitch = itemView.findViewById(R.id.enable_switch);
                editArrowButton = itemView.findViewById(R.id.edit_arrow_button);
            }
            
            public void bind(com.example.assistant.model.TimePeriod period, int index) {
                // 设置时段信息
                nameTextView.setText(period.getName());
                timeRangeTextView.setText(period.getStartTime() + " - " + period.getEndTime());
                // 根据重复类型和选择的日期设置显示文本
                String repeatText = "重复: ";
                switch (period.getRepeatType()) {
                    case DAILY:
                        repeatText += "每天";
                        break;
                    case WEEKLY:
                        List<Integer> selectedDays = period.getSelectedDays();
                        if (selectedDays.contains(1) && selectedDays.contains(2) && selectedDays.contains(3) &&
                            selectedDays.contains(4) && selectedDays.contains(5)) {
                            repeatText += "工作日";
                        } else if (selectedDays.contains(0) && selectedDays.contains(6)) {
                            repeatText += "周末";
                        } else {
                            repeatText += getDaysText(selectedDays);
                        }
                        break;
                    case SELECTED_DAYS:
                        repeatText += getDaysText(period.getSelectedDays());
                        break;
                    case ONCE:
                        repeatText += "一次";
                        break;
                    default:
                        repeatText += period.getRepeatType().toString();
                }
                repeatTextView.setText(repeatText);
                enableSwitch.setChecked(period.isEnabled());
                
                // 设置开关状态变化监听器
                enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    timePeriods.get(index).setEnabled(isChecked);
                    saveTimePeriods();
                });
                
                // 设置编辑箭头按钮点击事件
                editArrowButton.setOnClickListener(v -> showEditPeriodDialog(index));
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTimePeriodSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        meViewModel = new ViewModelProvider(requireActivity()).get(MeViewModel.class);
        meViewModel.initDatabase(requireContext());

        // 初始化UI组件
        timePeriodContainer = binding.timePeriodContainer;
        
        // 设置RecyclerView
        timePeriodContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimePeriodAdapter();
        timePeriodContainer.setAdapter(adapter);

        // 设置添加时段按钮点击事件
        binding.addPeriodButton.setOnClickListener(v -> showAddPeriodDialog());

        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener(v -> navigateBack());

        // 加载已保存的时段设置
        observeTimePeriods();

        return root;
    }

    /**
     * 从ViewModel观察时段数据变化
     */
    private void observeTimePeriods() {
        meViewModel.getTimePeriods().observe(getViewLifecycleOwner(), new Observer<List<com.example.assistant.model.TimePeriod>>() {
            @Override
            public void onChanged(List<com.example.assistant.model.TimePeriod> periods) {
                timePeriods = new ArrayList<>(periods);
                if (timePeriods.isEmpty()) {
                    initializeTimePeriods();
                }
                displayTimePeriods();
            }
        });
    }

    /**
     * 显示所有时段
     */
    private void displayTimePeriods() {
        // 更新适配器数据
        adapter.notifyDataSetChanged();
        
        // 设置左滑删除功能
        setupSwipeToDelete();
    }
    
    /**
     * 保存时段设置到ViewModel
     */
    private void saveTimePeriods() {
        meViewModel.saveTimePeriods(timePeriods);
        Toast.makeText(getContext(), "设置已保存", Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置左滑删除功能
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                
                // 显示确认删除对话框
                new AlertDialog.Builder(requireContext())
                        .setTitle("确认删除")
                        .setMessage("确定要删除这个时段限制吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            timePeriods.remove(position);
                            adapter.notifyItemRemoved(position);
                            saveTimePeriods();
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }
        };
        
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(timePeriodContainer);
    }

    /**
     * 显示添加时段对话框
     */
    private void showAddPeriodDialog() {
        final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_period, null);
        
        final EditText periodName = dialogView.findViewById(R.id.period_name_edit);
        final TextView startTimeText = dialogView.findViewById(R.id.start_time_text);
        final TextView endTimeText = dialogView.findViewById(R.id.end_time_text);
        final Spinner repeatSpinner = dialogView.findViewById(R.id.repeat_spinner);
        final CheckBox enableCheckBox = dialogView.findViewById(R.id.enable_checkbox);

        // 设置重复选项适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(adapter);

        // 设置默认时间
        startTimeText.setText("22:00");
        endTimeText.setText("06:00");

        // 设置开始时间选择器
        startTimeText.setOnClickListener(v -> {
            showTimePickerDialog((timePicker, hourOfDay, minute) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute);
                startTimeText.setText(time);
            });
        });

        // 设置结束时间选择器
        endTimeText.setOnClickListener(v -> {
            showTimePickerDialog((timePicker, hourOfDay, minute) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute);
                endTimeText.setText(time);
            });
        });

        // 显示对话框
        new AlertDialog.Builder(requireContext())
                .setTitle("添加禁止使用时段")
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = periodName.getText().toString().trim();
                    String startTime = startTimeText.getText().toString();
                    String endTime = endTimeText.getText().toString();
                    String repeat = repeatSpinner.getSelectedItem().toString();
                    boolean enabled = enableCheckBox.isChecked();

                    // 如果名称为空，使用默认名称
                    if (name.isEmpty()) {
                        name = "新增时段";
                    }

                    // 添加新时段
                    com.example.assistant.model.TimePeriod newPeriod = new com.example.assistant.model.TimePeriod();
                    newPeriod.setName(name);
                    newPeriod.setStartTime(startTime);
                    newPeriod.setEndTime(endTime);
                    // 根据选择的字符串设置重复类型
                    if ("每天".equals(repeat)) {
                        newPeriod.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.DAILY);
                    } else if ("工作日".equals(repeat)) {
                        newPeriod.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.WEEKLY);
                        // 工作日: 周一到周五
                        List<Integer> weekdays = new ArrayList<>();
                        weekdays.add(1); // 周一
                        weekdays.add(2); // 周二
                        weekdays.add(3); // 周三
                        weekdays.add(4); // 周四
                        weekdays.add(5); // 周五
                        newPeriod.setSelectedDays(weekdays);
                    } else if ("周末".equals(repeat)) {
                        newPeriod.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.WEEKLY);
                        // 周末: 周六和周日
                        List<Integer> weekends = new ArrayList<>();
                        weekends.add(6); // 周六
                        weekends.add(0); // 周日
                        newPeriod.setSelectedDays(weekends);
                    } else {
                        newPeriod.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.SELECTED_DAYS);
                    }
                    newPeriod.setEnabled(enabled);
                    timePeriods.add(newPeriod);
                    displayTimePeriods();
                    saveTimePeriods();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示编辑时段对话框
     */
    private void showEditPeriodDialog(final int index) {
        final com.example.assistant.model.TimePeriod period = timePeriods.get(index);
        final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_period, null);
        
        final EditText periodName = dialogView.findViewById(R.id.period_name_edit);
        final TextView startTimeText = dialogView.findViewById(R.id.start_time_text);
        final TextView endTimeText = dialogView.findViewById(R.id.end_time_text);
        final Spinner repeatSpinner = dialogView.findViewById(R.id.repeat_spinner);
        final CheckBox enableCheckBox = dialogView.findViewById(R.id.enable_checkbox);

        // 设置重复选项适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(adapter);

        // 填充现有数据
        periodName.setText(period.getName());
        startTimeText.setText(period.getStartTime());
        endTimeText.setText(period.getEndTime());
        enableCheckBox.setChecked(period.isEnabled());

        // 设置重复选项
        String repeatTypeStr = period.getRepeatType().toString();
        for (int i = 0; i < repeatSpinner.getCount(); i++) {
            if (repeatSpinner.getItemAtPosition(i).toString().equals(repeatTypeStr)) {
                repeatSpinner.setSelection(i);
                break;
            }
        }

        // 设置开始时间选择器
        startTimeText.setOnClickListener(v -> {
            showTimePickerDialog((timePicker, hourOfDay, minute) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute);
                startTimeText.setText(time);
            });
        });

        // 设置结束时间选择器
        endTimeText.setOnClickListener(v -> {
            showTimePickerDialog((timePicker, hourOfDay, minute) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute);
                endTimeText.setText(time);
            });
        });

        // 显示对话框
        new AlertDialog.Builder(requireContext())
                .setTitle("编辑禁止使用时段")
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = periodName.getText().toString().trim();
                    String startTime = startTimeText.getText().toString();
                    String endTime = endTimeText.getText().toString();
                    String repeat = repeatSpinner.getSelectedItem().toString();
                    boolean enabled = enableCheckBox.isChecked();

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入时段名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 更新时段信息
                    period.setName(name);
                    period.setStartTime(startTime);
                    period.setEndTime(endTime);
                    // 根据选择的字符串设置重复类型
                    if ("每天".equals(repeat)) {
                        period.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.DAILY);
                    } else if ("工作日".equals(repeat)) {
                        period.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.WEEKLY);
                        // 工作日: 周一到周五
                        List<Integer> weekdays = new ArrayList<>();
                        weekdays.add(1); // 周一
                        weekdays.add(2); // 周二
                        weekdays.add(3); // 周三
                        weekdays.add(4); // 周四
                        weekdays.add(5); // 周五
                        period.setSelectedDays(weekdays);
                    } else if ("周末".equals(repeat)) {
                        period.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.WEEKLY);
                        // 周末: 周六和周日
                        List<Integer> weekends = new ArrayList<>();
                        weekends.add(6); // 周六
                        weekends.add(0); // 周日
                        period.setSelectedDays(weekends);
                    } else {
                        period.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.SELECTED_DAYS);
                    }
                    period.setEnabled(enabled);
                    displayTimePeriods();
                    saveTimePeriods();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示时间选择器对话框
     */
    private void showTimePickerDialog(TimePickerDialog.OnTimeSetListener listener) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                listener,
                hour,
                minute,
                true // 24小时制
        );
        timePickerDialog.show();
    }



    /**
     * 导航回上一个页面
     */
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_me);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 初始化时段列表
     */
    private void initializeTimePeriods() {
        // 如果时段列表为空，添加一个默认的晚间休息时段
        if (timePeriods.isEmpty()) {
            com.example.assistant.model.TimePeriod defaultPeriod = new com.example.assistant.model.TimePeriod();
            defaultPeriod.setName("晚间休息");
            defaultPeriod.setStartTime("22:00");
            defaultPeriod.setEndTime("06:00");
            defaultPeriod.setRepeatType(com.example.assistant.model.TimePeriod.RepeatType.DAILY);
            defaultPeriod.setSelectedDays(new ArrayList<>());
            defaultPeriod.setEnabled(true);
            timePeriods.add(defaultPeriod);
            saveTimePeriods();
        }
    }

    /**
     * 根据选定的日期列表生成中文文本
     */
    private String getDaysText(List<Integer> days) {
        if (days == null || days.isEmpty()) {
            return "无";
        }
        
        StringBuilder daysText = new StringBuilder();
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        
        for (int day : days) {
            if (day >= 0 && day < dayNames.length) {
                if (daysText.length() > 0) {
                    daysText.append(", ");
                }
                daysText.append(dayNames[day]);
            }
        }
        
        return daysText.toString();
    }
}