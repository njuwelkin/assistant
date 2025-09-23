package com.example.assistant.ui.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentDailyTimeLimitBinding;

public class DailyTimeLimitFragment extends Fragment {

    private FragmentDailyTimeLimitBinding binding;
    private MeViewModel meViewModel;
    private int currentHours = 2; // 默认2小时
    private int currentMinutes = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDailyTimeLimitBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        meViewModel = new ViewModelProvider(requireActivity()).get(MeViewModel.class);
        meViewModel.initDatabase(requireContext());

        // 从ViewModel获取当前设置的每日使用时长
        Integer savedTimeLimit = meViewModel.getDailyTimeLimit().getValue();
        if (savedTimeLimit != null) {
            currentHours = savedTimeLimit / 60;
            currentMinutes = savedTimeLimit % 60;
            // 确保分钟值是10的倍数
            currentMinutes = Math.round(currentMinutes / 10.0f) * 10;
        }

        // 更新UI显示当前设置
        updateTimeDisplay();

        // 设置增减按钮点击事件
        binding.hoursIncreaseButton.setOnClickListener(v -> increaseHours());
        binding.hoursDecreaseButton.setOnClickListener(v -> decreaseHours());
        binding.minutesIncreaseButton.setOnClickListener(v -> increaseMinutes());
        binding.minutesDecreaseButton.setOnClickListener(v -> decreaseMinutes());

        // 设置确认按钮点击事件
        binding.confirmButton.setOnClickListener(v -> handleConfirmButtonClick());

        // 设置取消按钮点击事件
        binding.cancelButton.setOnClickListener(v -> handleCancelButtonClick());

        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener(v -> handleBackButtonClick());

        return root;
    }

    private void updateTimeDisplay() {
        binding.hoursValue.setText(String.valueOf(currentHours));
        binding.minutesValue.setText(String.valueOf(currentMinutes));
    }

    private void increaseHours() {
        if (currentHours < 23) { // 最大23小时
            currentHours++;
            updateTimeDisplay();
        }
    }

    private void decreaseHours() {
        if (currentHours > 0) {
            currentHours--;
            updateTimeDisplay();
        }
    }

    private void increaseMinutes() {
        if (currentMinutes < 50) {
            currentMinutes += 10;
        } else {
            currentMinutes = 0;
            if (currentHours < 23) {
                currentHours++;
            }
        }
        updateTimeDisplay();
    }

    private void decreaseMinutes() {
        if (currentMinutes > 0) {
            currentMinutes -= 10;
        } else {
            currentMinutes = 50;
            if (currentHours > 0) {
                currentHours--;
            }
        }
        updateTimeDisplay();
    }

    private void handleConfirmButtonClick() {
        // 计算总分钟数
        int totalMinutes = currentHours * 60 + currentMinutes;
        
        // 保存设置
        meViewModel.setDailyTimeLimit(requireContext(), totalMinutes);
        
        // 显示成功提示
        Toast.makeText(requireContext(), "每日使用时长设置成功", Toast.LENGTH_SHORT).show();
        
        // 导航回MeFragment
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_me);
    }

    private void handleCancelButtonClick() {
        // 导航回MeFragment
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_me);
    }

    private void handleBackButtonClick() {
        // 导航回MeFragment
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_me);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}