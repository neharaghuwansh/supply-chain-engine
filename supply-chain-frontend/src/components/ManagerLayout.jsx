import Sidebar from "./Sidebar";
import "./ManagerLayout.css";

function ManagerLayout({ children }) {
    return (
        <div className="manager-layout">

            <Sidebar />

            <main className="main-content">
                {children}
            </main>

        </div>
    );
}

export default ManagerLayout;