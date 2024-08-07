#include <wx/wx.h>
#include "app.h"
#include "MainFrame.h"

wxIMPLEMENT_APP(app);

bool app::OnInit()
{
	MainFrame* frame = new MainFrame("ytmp");
	frame->SetSize(800, 600);
	frame->Center();
	frame->Show(true);
	return true;
}