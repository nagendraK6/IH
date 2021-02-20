package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Bundle;

import com.relylabs.InstaHelo.R;

import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;

public class GetStartedFragment extends Fragment {

    public FragmentActivity activity_ref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_get_started, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView next_click = view.findViewById(R.id.get_started);
        next_click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadFragment(new LoginFragment());
                nextScreen(activity_ref);
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }

    private void loadFragment(Fragment fragment_to_start) {
        if (activity_ref != null) {
            FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_holder, fragment_to_start);
            ft.commit();
        }
    }
}