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
import com.example.assistant.databinding.FragmentParentPasswordBinding;

public class ParentPasswordFragment extends Fragment {

    private static final String TAG = "ParentPasswordFragment";
    private FragmentParentPasswordBinding binding;
    private MeViewModel meViewModel;
    private boolean isSettingPassword = false; // 用于标记是首次设置密码还是修改密码

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentParentPasswordBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        meViewModel = new ViewModelProvider(requireActivity()).get(MeViewModel.class);
        
        // 初始化数据库
        meViewModel.initDatabase(requireContext());

        // 检查是否已经设置了家长密码
        checkPasswordStatus();

        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // 设置确认按钮点击事件
        binding.confirmButton.setOnClickListener(v -> {
            handleConfirmButtonClick();
        });

        // 设置取消按钮点击事件
        binding.cancelButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return root;
    }

    /**
     * 检查家长密码状态，决定显示设置还是修改界面
     */
    private void checkPasswordStatus() {
        Boolean isPasswordSet = meViewModel.isParentPasswordSet().getValue();
        if (isPasswordSet != null && !isPasswordSet) {
            // 首次设置密码
            isSettingPassword = true;
            binding.titleText.setText("设置家长密码");
            binding.oldPasswordLayout.setVisibility(View.GONE);
            binding.newPasswordLayout.setVisibility(View.VISIBLE);
            binding.confirmPasswordLayout.setVisibility(View.VISIBLE);
        } else {
            // 修改密码
            isSettingPassword = false;
            binding.titleText.setText("修改家长密码");
            binding.oldPasswordLayout.setVisibility(View.VISIBLE);
            binding.newPasswordLayout.setVisibility(View.VISIBLE);
            binding.confirmPasswordLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 处理确认按钮点击事件
     */
    private void handleConfirmButtonClick() {
        if (isSettingPassword) {
            // 首次设置密码
            String newPassword = binding.newPasswordEditText.getText().toString();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString();

            if (validateNewPassword(newPassword, confirmPassword)) {
                // 设置新密码
                meViewModel.setParentPassword(newPassword);
                // 检查密码是否设置成功
                Boolean isPasswordSet = meViewModel.isParentPasswordSet().getValue();
                if (isPasswordSet != null && isPasswordSet) {
                    Toast.makeText(requireContext(), "家长密码设置成功", Toast.LENGTH_SHORT).show();
                    // 明确导航到MeFragment，而不是简单地popBackStack
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_me);
                } else {
                    Toast.makeText(requireContext(), "密码设置失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // 修改密码
            String oldPassword = binding.oldPasswordEditText.getText().toString();
            String newPassword = binding.newPasswordEditText.getText().toString();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString();

            // 验证原密码
            if (meViewModel.verifyParentPassword(oldPassword)) {
                // 验证新密码
                if (validateNewPassword(newPassword, confirmPassword)) {
                    // 更新密码
                    meViewModel.setParentPassword(newPassword);
                    // 检查密码是否更新成功
                    Boolean isPasswordSet = meViewModel.isParentPasswordSet().getValue();
                    if (isPasswordSet != null && isPasswordSet) {
                    Toast.makeText(requireContext(), "家长密码修改成功", Toast.LENGTH_SHORT).show();
                    // 明确导航到MeFragment，而不是简单地popBackStack
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_me);
                } else {
                    Toast.makeText(requireContext(), "密码修改失败，请重试", Toast.LENGTH_SHORT).show();
                }
                }
            } else {
                Toast.makeText(requireContext(), "原密码不正确", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 验证新密码格式和两次输入是否一致
     */
    private boolean validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword.isEmpty()) {
            Toast.makeText(requireContext(), "请输入新密码", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(requireContext(), "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}