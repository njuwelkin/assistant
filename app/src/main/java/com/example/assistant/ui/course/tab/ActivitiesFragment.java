package com.example.assistant.ui.course.tab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentActivitiesBinding;

public class ActivitiesFragment extends Fragment {

    private FragmentActivitiesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 暂时保持空白，仅显示提示信息
        TextView emptyText = binding.emptyText;
        emptyText.setText(getString(R.string.activities_empty_text));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}