package com.example.assistant.ui.notifications;

import android.os.Bundle;
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
import com.example.assistant.databinding.FragmentParentPasswordVerifyBinding;

public class ParentPasswordVerifyFragment extends Fragment {

    private FragmentParentPasswordVerifyBinding binding;
    private MeViewModel meViewModel;
    private static final String ARG_DESTINATION = "destination";

    public static ParentPasswordVerifyFragment newInstance(int destination) {
        ParentPasswordVerifyFragment fragment = new ParentPasswordVerifyFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DESTINATION, destination);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentParentPasswordVerifyBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        meViewModel = new ViewModelProvider(requireActivity()).get(MeViewModel.class);
        meViewModel.initDatabase(requireContext());

        // 设置确认按钮点击事件
        binding.confirmButton.setOnClickListener(v -> handleConfirmButtonClick());

        // 设置取消按钮点击事件
        binding.cancelButton.setOnClickListener(v -> handleCancelButtonClick());

        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener(v -> handleBackButtonClick());

        return root;
    }

    private void handleConfirmButtonClick() {
        String password = binding.passwordEditText.getText().toString().trim();
        
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证家长密码
        boolean isPasswordCorrect = meViewModel.verifyParentPassword(password);
        
        if (isPasswordCorrect) {
            // 密码验证成功，导航到目标页面
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            
            // 检查参数类型并设置相应的目标页面
            if (getArguments() != null) {
                // 方式1：检查是否有字符串类型的next_destination参数
                String nextDestination = getArguments().getString("next_destination");
                if (nextDestination != null && !nextDestination.isEmpty()) {
                    if (nextDestination.equals("time_period_settings")) {
                        navController.navigate(R.id.time_period_settings_fragment);
                        return;
                    }
                }
                
                // 方式2：使用原有的整型参数
                int destination = getArguments().getInt(ARG_DESTINATION, R.id.daily_time_limit_fragment);
                navController.navigate(destination);
            } else {
                // 默认导航到每日使用时长页面
                navController.navigate(R.id.daily_time_limit_fragment);
            }
        } else {
            // 密码验证失败
            Toast.makeText(requireContext(), "密码错误，请重新输入", Toast.LENGTH_SHORT).show();
            binding.passwordEditText.setText("");
        }
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