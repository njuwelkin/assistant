package com.example.assistant.ui.notifications;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.assistant.LoginActivity;
import com.example.assistant.databinding.FragmentMeBinding;
import com.example.assistant.ui.chat.ChatViewModel;
import com.example.assistant.ui.notifications.MeViewModel;
import com.example.assistant.util.AuthManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MeFragment extends Fragment {

    private static final String TAG = "MeFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private FragmentMeBinding binding;
    private String currentPhotoPath;
    private MeViewModel meViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        meViewModel = new ViewModelProvider(this).get(MeViewModel.class);

        // 观察ViewModel中的数据变化并更新UI
        observeViewModelData();

        // 设置头像点击事件
        binding.avatarImage.setOnClickListener(v -> showAvatarOptions());

        // 设置家长密码设置点击事件
        binding.parentPasswordLayout.setOnClickListener(v -> navigateToParentPasswordSettings());

        // 设置个人资料编辑点击事件
        binding.profileEditLayout.setOnClickListener(v -> navigateToProfileEdit());

        // 设置每日使用时长点击事件
        binding.dailyTimeLimitLayout.setOnClickListener(v -> navigateToDailyTimeLimitSettings());

        // 设置使用时段限制点击事件
        binding.timePeriodLayout.setOnClickListener(v -> navigateToTimePeriodSettings());

        // 设置隐私设置点击事件
        binding.privacySettingsLayout.setOnClickListener(v -> navigateToPrivacySettings());

        // 设置清除缓存点击事件
        binding.clearCacheLayout.setOnClickListener(v -> showClearCacheConfirmation());

        // 设置退出按钮点击事件
        binding.logoutButton.setOnClickListener(v -> showLogoutConfirmation());

        return root;
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private void observeViewModelData() {
        // 观察用户名变化
        meViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            binding.usernameText.setText(username);
        });

        // 观察用户ID变化
        meViewModel.getUserId().observe(getViewLifecycleOwner(), userId -> {
            binding.userIdText.setText("ID: " + userId);
        });

        // 观察头像URI变化
        meViewModel.getAvatarUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null && !uri.isEmpty()) {
                binding.avatarImage.setImageURI(Uri.parse(uri));
            }
        });

        // 观察家长密码设置状态变化
        meViewModel.isParentPasswordSet().observe(getViewLifecycleOwner(), isSet -> {
            // 可以根据密码设置状态更新UI，比如显示一个已设置的图标
        });

        // 观察最后登录时间变化
        meViewModel.getLastLoginTime().observe(getViewLifecycleOwner(), loginTime -> {
            // 可以在UI中显示最后登录时间
        });

        // 观察每日使用时长限制
        meViewModel.getDailyTimeLimit().observe(getViewLifecycleOwner(), minutes -> {
            if (minutes != null) {
                int hours = minutes / 60;
                int mins = minutes % 60;
                if (mins > 0) {
                    binding.dailyTimeLimitValue.setText(hours + "小时" + mins + "分钟");
                } else {
                    binding.dailyTimeLimitValue.setText(hours + "小时");
                }
            }
        });

        // 观察使用时段设置
        meViewModel.getTimePeriodStart().observe(getViewLifecycleOwner(), startTime -> {
            updateTimePeriodDisplay();
        });

        meViewModel.getTimePeriodEnd().observe(getViewLifecycleOwner(), endTime -> {
            updateTimePeriodDisplay();
        });

        // 观察缓存大小
        meViewModel.getCacheSize().observe(getViewLifecycleOwner(), size -> {
            if (size != null) {
                binding.cacheSizeValue.setText(size);
            }
        });
    }

    /**
     * 更新使用时段显示
     */
    private void updateTimePeriodDisplay() {
        String startTime = meViewModel.getTimePeriodStart().getValue();
        String endTime = meViewModel.getTimePeriodEnd().getValue();
        if (startTime != null && endTime != null) {
            binding.timePeriodValue.setText(startTime + " - " + endTime);
        }
    }

    /**
     * 显示头像选择选项（拍照或从相册选择）
     */
    private void showAvatarOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("更换头像")
                .setItems(new String[]{"拍照", "从相册选择"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            takePhoto();
                        } else {
                            pickFromGallery();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 拍照功能
     */
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(requireContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * 从相册选择图片
     */
    private void pickFromGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
    }

    /**
     * 创建临时图片文件
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * 处理Activity结果
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK) {
            // 拍照成功，这里可以处理照片
            // 由于我们在拍照时指定了输出路径，可以直接使用currentPhotoPath
            if (currentPhotoPath != null) {
                // 这里可以进行图片处理，然后设置到头像上
                Toast.makeText(requireContext(), "头像已更新", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == getActivity().RESULT_OK && data != null) {
            // 从相册选择成功
            Uri selectedImage = data.getData();
            binding.avatarImage.setImageURI(selectedImage);
            Toast.makeText(requireContext(), "头像已更新", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 导航到家长密码设置页面
     */
    private void navigateToParentPasswordSettings() {
        // 这里可以实现跳转到家长密码设置页面的逻辑
        Toast.makeText(requireContext(), "跳转到家长密码设置页面", Toast.LENGTH_SHORT).show();
        // 实际项目中这里应该使用Navigation组件进行页面跳转
        // NavHostFragment.findNavController(this).navigate(R.id.action_navigation_me_to_parentPasswordFragment);
    }

    /**
     * 导航到个人资料编辑页面
     */
    private void navigateToProfileEdit() {
        // 这里可以实现跳转到个人资料编辑页面的逻辑
        Toast.makeText(requireContext(), "跳转到个人资料编辑页面", Toast.LENGTH_SHORT).show();
        // 实际项目中这里应该使用Navigation组件进行页面跳转
        // NavHostFragment.findNavController(this).navigate(R.id.action_navigation_me_to_profileEditFragment);
    }

    /**
     * 导航至每日使用时长设置
     */
    private void navigateToDailyTimeLimitSettings() {
        // 这里可以实现导航到每日使用时长设置页面的逻辑
        Toast.makeText(requireContext(), "每日使用时长设置", Toast.LENGTH_SHORT).show();
    }

    /**
     * 导航至使用时段限制设置
     */
    private void navigateToTimePeriodSettings() {
        // 这里可以实现导航到使用时段限制设置页面的逻辑
        Toast.makeText(requireContext(), "使用时段限制设置", Toast.LENGTH_SHORT).show();
    }

    /**
     * 导航至隐私设置
     */
    private void navigateToPrivacySettings() {
        // 这里可以实现导航到隐私设置页面的逻辑
        Toast.makeText(requireContext(), "隐私设置", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示清除缓存确认对话框
     */
    private void showClearCacheConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("清除缓存")
                .setMessage("确定要清除应用缓存吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    clearAppCache();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 清除应用缓存
     */
    private void clearAppCache() {
        try {
            // 简单模拟清除缓存的过程
            // 实际应用中应该根据应用的缓存目录进行清理
            Thread.sleep(500); // 模拟清理过程
            binding.cacheSizeValue.setText("0MB");
            Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "清除缓存失败", e);
            Toast.makeText(requireContext(), "清除缓存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示退出登录确认对话框
     */
    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 断开WebSocket连接以释放资源
                        try {
                            ChatViewModel chatViewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
                            chatViewModel.disconnectWebSocket();
                        } catch (Exception e) {
                            Log.d(TAG, "断开WebSocket连接失败", e);
                        }
                        
                        // 清除认证信息
                        AuthManager.clearAuthInfo(requireContext());
                        
                        // 跳转到登录页面
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}