package com.example.assistant.ui.me;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.model.TimePeriod;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TimePeriodSettingsFragment extends Fragment {

    private RecyclerView timePeriodContainer; // 注意这里变量类型改为RecyclerView
    private Button addPeriodButton;
    private ImageButton backButton;
    private List<TimePeriod> timePeriods = new ArrayList<>();
    private MeViewModel meViewModel;
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
            TimePeriod period = timePeriods.get(position);
            holder.bind(period, position);
        }
        
        @Override
        public int getItemCount() {
            return timePeriods.size();
        }
        
        class TimePeriodViewHolder extends RecyclerView.ViewHolder {
            TextView periodName;
            TextView timeRange;
            TextView repeatDays;
            Switch enableSwitch;
            TextView editArrowButton;
            
            public TimePeriodViewHolder(@NonNull View itemView) {
                super(itemView);
                periodName = itemView.findViewById(R.id.period_name);
                timeRange = itemView.findViewById(R.id.time_range);
                repeatDays = itemView.findViewById(R.id.repeat_days);
                enableSwitch = itemView.findViewById(R.id.enable_switch);
                editArrowButton = itemView.findViewById(R.id.edit_arrow_button);
            }
            
            public void bind(TimePeriod period, int position) {
                periodName.setText(period.getName());
                timeRange.setText(period.getStartTime() + " - " + period.getEndTime());
                repeatDays.setText(getRepeatText(period.getRepeatType(), period.getSelectedDays()));
                enableSwitch.setChecked(period.isEnabled());
                
                enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    period.setEnabled(isChecked);
                    saveTimePeriods();
                });
                
                // 设置编辑箭头按钮点击事件
                editArrowButton.setOnClickListener(v -> showTimePeriodDialog(period));
            }
        }
    }

    public static final String ARG_FROM_PASSWORD_VERIFICATION = "from_password_verification";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_time_period_settings, container, false);
        
        // 注意这里变量类型已改为RecyclerView
        timePeriodContainer = root.findViewById(R.id.timePeriodContainer);
        addPeriodButton = root.findViewById(R.id.addPeriodButton);
        backButton = root.findViewById(R.id.backButton);
        
        // 设置RecyclerView
        timePeriodContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimePeriodAdapter();
        timePeriodContainer.setAdapter(adapter);
        
        // 初始化ViewModel
        meViewModel = new ViewModelProvider(requireActivity()).get(MeViewModel.class);
        
        // 从参数中检查是否来自密码验证
        boolean fromPasswordVerification = getArguments() != null && 
                getArguments().getBoolean(ARG_FROM_PASSWORD_VERIFICATION, false);
        
        setupListeners();
        observeTimePeriods();
        
        return root;
    }

    private void setupListeners() {
        addPeriodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePeriodDialog(null);
            }
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }
    
    private void observeTimePeriods() {
        meViewModel.getTimePeriods().observe(getViewLifecycleOwner(), new Observer<List<TimePeriod>>() {
            @Override
            public void onChanged(List<TimePeriod> periods) {
                timePeriods = new ArrayList<>(periods);
                displayTimePeriods();
            }
        });
    }

    private void saveTimePeriods() {
        meViewModel.saveTimePeriods(timePeriods);
        Toast.makeText(getContext(), "设置已保存", Toast.LENGTH_SHORT).show();
    }

    private void displayTimePeriods() {
        if (timePeriods.isEmpty()) {
            // 如果列表为空，显示提示信息
            TextView emptyView = new TextView(getContext());
            emptyView.setText("暂无禁止使用时段，请点击添加按钮创建");
            emptyView.setTextSize(16);
            emptyView.setTextColor(getResources().getColor(R.color.gray));
            emptyView.setPadding(0, 40, 0, 0);
            emptyView.setGravity(View.TEXT_ALIGNMENT_CENTER);
            emptyView.setId(android.R.id.empty);
            
            // 清空RecyclerView并添加空视图
            timePeriodContainer.setAdapter(null);
            ((ViewGroup)timePeriodContainer.getParent()).addView(emptyView);
        } else {
            // 移除可能存在的空视图
            ViewGroup parent = (ViewGroup)timePeriodContainer.getParent();
            View emptyView = parent.findViewById(android.R.id.empty);
            if (emptyView != null) {
                parent.removeView(emptyView);
            }
            
            // 更新适配器数据
            adapter.notifyDataSetChanged();
            
            // 设置左滑删除功能
            setupSwipeToDelete();
        }
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
                final TimePeriod periodToDelete = timePeriods.get(position);
                
                // 显示确认删除对话框
                new AlertDialog.Builder(getContext())
                        .setTitle("确认删除")
                        .setMessage("确定要删除此时段吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            timePeriods.remove(position);
                            adapter.notifyItemRemoved(position);
                            saveTimePeriods();
                            
                            // 检查是否需要显示空视图
                            if (timePeriods.isEmpty()) {
                                displayTimePeriods();
                            }
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

    private String getRepeatText(TimePeriod.RepeatType repeatType, List<Integer> selectedDays) {
        switch (repeatType) {
            case ONCE:
                return "一次性";
            case DAILY:
                return "每天";
            case WEEKLY:
                return "每周";
            case SELECTED_DAYS:
                if (selectedDays != null && !selectedDays.isEmpty()) {
                    StringBuilder sb = new StringBuilder("自定义：");
                    String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                    for (int i = 0; i < selectedDays.size(); i++) {
                        int dayOfWeek = selectedDays.get(i);
                        if (dayOfWeek >= 0 && dayOfWeek < weekdays.length) {
                            sb.append(weekdays[dayOfWeek]);
                            if (i < selectedDays.size() - 1) {
                                sb.append(", ");
                            }
                        }
                    }
                    return sb.toString();
                }
                return "自定义";
            default:
                return "每天";
        }
    }

    private void showTimePeriodDialog(final TimePeriod periodToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(periodToEdit == null ? "添加时段" : "编辑时段");
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_time_period, null);
        EditText periodNameEdit = dialogView.findViewById(R.id.period_name_edit);
        TextView startTimeText = dialogView.findViewById(R.id.start_time_text);
        TextView endTimeText = dialogView.findViewById(R.id.end_time_text);
        Spinner repeatSpinner = dialogView.findViewById(R.id.repeat_spinner);
        CheckBox enableCheckbox = dialogView.findViewById(R.id.enable_checkbox);
        
        // 填充现有数据（如果是编辑模式）
        if (periodToEdit != null) {
            periodNameEdit.setText(periodToEdit.getName());
            startTimeText.setText(periodToEdit.getStartTime());
            endTimeText.setText(periodToEdit.getEndTime());
            enableCheckbox.setChecked(periodToEdit.isEnabled());
        } else {
            // 设置默认时间
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String currentTime = sdf.format(new Date());
            startTimeText.setText(currentTime);
            
            // 设置结束时间为开始时间后1小时
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            endTimeText.setText(sdf.format(calendar.getTime()));
        }
        
        // 设置重复选项适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.repeat_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(adapter);
        
        if (periodToEdit != null) {
            // 设置现有重复选项
            switch (periodToEdit.getRepeatType()) {
                case ONCE:
                    repeatSpinner.setSelection(0);
                    break;
                case DAILY:
                    repeatSpinner.setSelection(1);
                    break;
                case WEEKLY:
                    repeatSpinner.setSelection(2);
                    break;
                case SELECTED_DAYS:
                    repeatSpinner.setSelection(3);
                    break;
            }
        }
        
        // 时间选择器
        startTimeText.setOnClickListener(v -> showTimePicker(startTimeText, periodToEdit != null ? periodToEdit.getStartTime() : ""));
        endTimeText.setOnClickListener(v -> showTimePicker(endTimeText, periodToEdit != null ? periodToEdit.getEndTime() : ""));
        
        builder.setView(dialogView)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = periodNameEdit.getText().toString().trim();
                        String startTime = startTimeText.getText().toString();
                        String endTime = endTimeText.getText().toString();
                        int repeatPosition = repeatSpinner.getSelectedItemPosition();
                        boolean enabled = enableCheckbox.isChecked();
                        
                        // 如果名称为空，使用默认名称
                        if (name.isEmpty()) {
                            name = "新增时段";
                        }
                        
                        TimePeriod.RepeatType repeatType;
                        List<Integer> selectedDays = null;
                        
                        switch (repeatPosition) {
                            case 0:
                                repeatType = TimePeriod.RepeatType.ONCE;
                                break;
                            case 1:
                                repeatType = TimePeriod.RepeatType.DAILY;
                                break;
                            case 2:
                                repeatType = TimePeriod.RepeatType.WEEKLY;
                                break;
                            case 3:
                                repeatType = TimePeriod.RepeatType.SELECTED_DAYS;
                                selectedDays = new ArrayList<>();
                                // 这里可以进一步实现选择特定日期的功能
                                break;
                            default:
                                repeatType = TimePeriod.RepeatType.DAILY;
                        }
                        
                        if (periodToEdit != null) {
                            // 编辑模式
                            periodToEdit.setName(name);
                            periodToEdit.setStartTime(startTime);
                            periodToEdit.setEndTime(endTime);
                            periodToEdit.setRepeatType(repeatType);
                            periodToEdit.setSelectedDays(selectedDays);
                            periodToEdit.setEnabled(enabled);
                        } else {
                            // 添加模式
                            TimePeriod newPeriod = new TimePeriod(
                                    name, startTime, endTime, repeatType, selectedDays, enabled);
                            timePeriods.add(newPeriod);
                        }
                        
                        displayTimePeriods();
                        saveTimePeriods();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showTimePicker(final TextView timeTextView, String initialTime) {
        Calendar calendar = Calendar.getInstance();
        
        if (!initialTime.isEmpty()) {
            try {
                String[] parts = initialTime.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                calendar.set(Calendar.HOUR_OF_DAY, hours);
                calendar.set(Calendar.MINUTE, minutes);
            } catch (Exception e) {
                // 默认使用当前时间
            }
        }
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String formattedTime = String.format("%02d:%02d", hourOfDay, minute);
                        timeTextView.setText(formattedTime);
                    }
                },
                hour, minute, true);
        
        timePickerDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 确保所有更改都保存
        saveTimePeriods();
    }
}