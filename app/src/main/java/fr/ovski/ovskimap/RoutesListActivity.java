package fr.ovski.ovskimap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import fr.ovski.ovskimap.models.Route;

public class RoutesListActivity extends AppCompatActivity {

    private class RouteViewHolder extends RecyclerView.ViewHolder {
        private View view;

        RouteViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            view.setOnClickListener(view -> Toast.makeText(view.getContext(), "on click", Toast.LENGTH_LONG).show());

        }

        void setName(String name) {
            TextView textView = view.findViewById(R.id.route_name);
            textView.setText(name);
        }
    }
    private FirestoreRecyclerAdapter<Route, RouteViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes_list);

        RecyclerView recyclerView = findViewById(R.id.routes_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        Query query = rootRef.collection("users").document(user.getUid()).collection("routes");
        FirestoreRecyclerOptions<Route> options = new FirestoreRecyclerOptions.Builder<Route>()
                .setQuery(query, Route.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Route, RouteViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RouteViewHolder holder, int position, @NonNull Route route) {
                holder.setName(route.getName());
            }

            @NonNull
            @Override
            public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
                return new RouteViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (adapter != null) {
            adapter.stopListening();
        }
    }
}