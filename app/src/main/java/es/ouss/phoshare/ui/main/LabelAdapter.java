package es.ouss.phoshare.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import es.ouss.phoshare.databinding.ItemLabelBinding;

/** One line of text in a list, shown once there is text to show. */
public class LabelAdapter extends RecyclerView.Adapter<LabelAdapter.Holder> {

    private CharSequence text;

    public void setText(CharSequence text) {
        boolean had = this.text != null;
        this.text = text;
        if (had) {
            notifyItemChanged(0);
        } else {
            notifyItemInserted(0);
        }
    }

    @Override
    public int getItemCount() {
        return text == null ? 0 : 1;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLabelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.binding.label.setText(text);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemLabelBinding binding;

        Holder(ItemLabelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
