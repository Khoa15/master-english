package com.masterenglish.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentAuthBinding;
import com.masterenglish.app.data.repository.AuthRepository;

import dagger.hilt.android.AndroidEntryPoint;

/** Email/password, Google, or Guest Mode. Success moves to the roadmap and clears the auth stack. */
@AndroidEntryPoint
public class AuthFragment extends Fragment {

    private FragmentAuthBinding binding;
    private AuthViewModel viewModel;
    private boolean registerMode;

    private final ActivityResultLauncher<android.content.Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account.getIdToken() != null) {
                        viewModel.loginWithGoogle(account.getIdToken());
                    } else {
                        binding.statusText.setText(R.string.auth_title);
                    }
                } catch (ApiException e) {
                    binding.statusText.setText(getString(R.string.auth_title) + " (" + e.getStatusCode() + ")");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        boolean ready = viewModel.firebaseReady();
        binding.statusText.setText(ready ? "" : AuthRepository.NOT_CONFIGURED);
        binding.primaryButton.setEnabled(ready);
        binding.googleButton.setEnabled(ready);
        binding.switchModeButton.setOnClickListener(v -> toggleMode());
        binding.primaryButton.setOnClickListener(v -> viewModel.login(
                text(binding.emailInput), text(binding.passwordInput), registerMode));
        binding.googleButton.setOnClickListener(v -> googleLauncher.launch(viewModel.googleClient().getSignInIntent()));
        binding.guestButton.setOnClickListener(v -> viewModel.continueAsGuest());

        viewModel.loading().observe(getViewLifecycleOwner(), busy ->
                binding.progress.setVisibility(Boolean.TRUE.equals(busy) ? View.VISIBLE : View.GONE));
        viewModel.error().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                binding.statusText.setText(message);
            }
        });
        viewModel.signedInUser().observe(getViewLifecycleOwner(), userId -> {
            if (userId != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_auth_to_roadmap);
            }
        });
        applyMode();
    }

    private void toggleMode() {
        registerMode = !registerMode;
        applyMode();
    }

    private void applyMode() {
        binding.primaryButton.setText(registerMode ? R.string.auth_register : R.string.auth_login);
        binding.switchModeButton.setText(registerMode ? R.string.auth_switch_to_login : R.string.auth_switch_to_register);
    }

    private static String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
