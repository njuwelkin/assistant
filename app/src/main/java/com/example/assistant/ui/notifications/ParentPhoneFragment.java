/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
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
import com.example.assistant.databinding.FragmentParentPhoneBinding;

/**
 * 家长电话设置Fragment
 */
public class ParentPhoneFragment extends Fragment {

    private FragmentParentPhoneBinding binding;
    private MeViewModel meViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentParentPhoneBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        meViewModel = new ViewModelProvider(requireActivity()).get(MeViewModel.class);
        meViewModel.initDatabase(requireContext());

        // 从ViewModel获取当前保存的家长电话号码
        String savedPhoneNumber = meViewModel.getParentPhoneNumber().getValue();
        if (savedPhoneNumber != null && !savedPhoneNumber.isEmpty()) {
            binding.phoneEditText.setText(savedPhoneNumber);
        }

        // 设置确认按钮点击事件
        binding.confirmButton.setOnClickListener(v -> handleConfirmButtonClick());

        // 设置取消按钮点击事件
        binding.cancelButton.setOnClickListener(v -> handleCancelButtonClick());

        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener(v -> handleBackButtonClick());

        return root;
    }

    private void handleConfirmButtonClick() {
        // 获取输入的电话号码
        String phoneNumber = binding.phoneEditText.getText().toString().trim();
        
        // 验证电话号码是否为11位
        if (!isValidPhoneNumber(phoneNumber)) {
            Toast.makeText(requireContext(), "请输入11位手机号码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存设置
        meViewModel.setParentPhoneNumber(requireContext(), phoneNumber);
        
        // 显示成功提示
        Toast.makeText(requireContext(), "家长电话设置成功", Toast.LENGTH_SHORT).show();
        
        // 导航回MeFragment
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_me);
    }

    /**
     * 验证电话号码是否为11位
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^1\\d{10}$");
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